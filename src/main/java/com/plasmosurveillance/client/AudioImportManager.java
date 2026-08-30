package com.plasmosurveillance.client;

import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.Config;
import com.plasmosurveillance.network.NetworkHandler;
import com.plasmosurveillance.network.TapeImportBeginPacket;
import com.plasmosurveillance.network.TapeImportChunkPacket;
import com.plasmosurveillance.network.TapeImportEndPacket;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Client-side file -> 48kHz mono PCM importer. */
public final class AudioImportManager {
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PlasmoTapeImport");t.setDaemon(true);return t;});
    private static volatile boolean importing=false;
    private static volatile boolean choosing=false;
    private static volatile String fileName="";
    private static volatile long sentBytes=0,totalBytes=0;
    private static volatile String status="";

    private AudioImportManager(){}
    public static boolean isImporting(){return importing;}
    public static boolean isChoosing(){return choosing;}
    public static String getFileName(){return fileName;}
    public static long getSentBytes(){return sentBytes;}
    public static long getTotalBytes(){return totalBytes;}
    public static String getStatus(){return status;}

    /**
     * Opens a Windows native file dialog without using java.awt.
     * Minecraft/Forge runs with java.awt in headless mode, so FileDialog/JFileChooser
     * throws HeadlessException. The dialog is launched off the render thread.
     */
    public static void chooseFile(){
        if(importing || choosing)return;
        choosing=true;
        status="Select a WAV file...";
        EXECUTOR.execute(() -> {
            try {
                String command="$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Windows.Forms; " +
                        "$d=New-Object System.Windows.Forms.OpenFileDialog; " +
                        "$d.Title='Select audio for Tape Player'; " +
                        "$d.Filter='WAV audio (*.wav;*.wave)|*.wav;*.wave|All files (*.*)|*.*'; " +
                        "$d.Multiselect=$false; " +
                        "if($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK){[Console]::Write($d.FileName)}";
                Process p=new ProcessBuilder("powershell.exe","-NoProfile","-STA","-Command",command)
                        .redirectErrorStream(true).start();
                String selected;
                try(InputStream in=p.getInputStream()){
                    selected=new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();
                }
                int exit=p.waitFor();
                if(exit!=0) throw new IOException("File picker failed (exit "+exit+")");
                if(!selected.isEmpty()){
                    Path path=Path.of(selected);
                    if(path.toString().toLowerCase().endsWith(".wav") || path.toString().toLowerCase().endsWith(".wave")){
                        Minecraft.getInstance().execute(() -> importFile(path));
                    }else{
                        Minecraft.getInstance().execute(() -> status="Only WAV audio is supported right now.");
                    }
                }else Minecraft.getInstance().execute(() -> status="File selection cancelled.");
            }catch(Exception e){
                PlasmoSurveillance.LOGGER.error("Audio file picker failed",e);
                Minecraft.getInstance().execute(() -> status="Could not open file picker.");
            }finally{
                choosing=false;
            }
        });
    }

    public static void importFile(Path path){
        if(importing || path==null)return;
        importing=true; fileName=path.getFileName().toString(); sentBytes=0; totalBytes=0; status="Reading audio...";
        EXECUTOR.execute(() -> {
            UUID id=UUID.randomUUID();
            try(AudioInputStream source=AudioSystem.getAudioInputStream(path.toFile())){
                AudioFormat target=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,48000f,16,1,2,48000f,false);
                if(!AudioSystem.isConversionSupported(target,source.getFormat())) throw new IOException("Unsupported audio format. Please use WAV/PCM.");
                try(AudioInputStream pcm=AudioSystem.getAudioInputStream(target,source)){
                    final long maxSamples=(long)Config.MAX_IMPORT_LENGTH_SECONDS.get()*48000L;
                    final long declaredFrames=pcm.getFrameLength();
                    if(declaredFrames>maxSamples)
                        throw new IOException("Audio is longer than the " + Config.MAX_IMPORT_LENGTH_SECONDS.get() / 60 + " minute import limit.");
                    totalBytes=declaredFrames>0?declaredFrames*2:0;
                    NetworkHandler.CHANNEL.sendToServer(new TapeImportBeginPacket(id,fileName));
                    byte[] buffer=new byte[32768]; int seq=0,n; long total=0;
                    while((n=pcm.read(buffer))!=-1){
                        if(n==0)continue;
                        total+=n;
                        long samplesRead=total/2L;
                        if(samplesRead>maxSamples)
                            throw new IOException("Audio is longer than the " + Config.MAX_IMPORT_LENGTH_SECONDS.get() / 60 + " minute import limit.");
                        byte[] chunk=java.util.Arrays.copyOf(buffer,n);
                        NetworkHandler.CHANNEL.sendToServer(new TapeImportChunkPacket(id,seq++,chunk));
                        sentBytes=total; status="Uploading...";
                    }
                    NetworkHandler.CHANNEL.sendToServer(new TapeImportEndPacket(id));
                    status="Import complete";
                }
            }catch(Exception e){
                status="Import failed: "+e.getMessage();
                PlasmoSurveillance.LOGGER.error("Audio import failed for {}",path,e);
            }finally{ importing=false; }
        });
    }

    public static void handleDrop(String file){
        if(file==null || importing || choosing)return;
        String lower=file.toLowerCase();
        if(lower.endsWith(".wav") || lower.endsWith(".wave")) importFile(Path.of(file));
        else status="Only WAV audio is supported right now.";
    }
}
