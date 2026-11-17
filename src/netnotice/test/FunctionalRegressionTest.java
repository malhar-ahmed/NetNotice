package netnotice.test;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import netnotice.common.*;
import netnotice.server.NetNoticeServer;

/** Regression coverage for text framing, quiz privacy, validation and restart lifecycle. */
public class FunctionalRegressionTest {
    static void check(boolean condition,String name) { if(!condition)throw new AssertionError(name); System.out.println("PASS: "+name); }
    static final class Peer implements AutoCloseable {
        final Socket socket; final BufferedReader in; final PrintWriter out;
        Peer(int port,String name) throws Exception {
            socket=new Socket("127.0.0.1",port); socket.setSoTimeout(2000);
            in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            out=new PrintWriter(socket.getOutputStream(),true,StandardCharsets.UTF_8);
            check(in.readLine().startsWith(Protocol.PREFIX_MESSAGE),"Welcome received");
            out.println(Protocol.PREFIX_JOIN+name);
        }
        public void close() throws Exception { socket.close(); }
    }
    public static void main(String[] args) throws Exception {
        int port; try(ServerSocket free=new ServerSocket(0)) {port=free.getLocalPort();}
        try {
            NetNoticeServer.startServer(port);
            try(Peer a=new Peer(port,"Alice")) {
                check(a.in.readLine().equals("JOINED|Alice"),"Registration");
                try(Peer duplicate=new Peer(port,"Alice")) { check(duplicate.in.readLine().startsWith("ERROR|"),"Duplicate names rejected"); }
                String notice="First line\nSecond line | café — සිංහල\r\nThird line";
                NetNoticeServer.broadcastAnnouncement(notice);
                String wire=a.in.readLine();
                check(Protocol.decode(wire.substring(Protocol.PREFIX_ANNOUNCEMENT.length())).equals(notice),"Multiline Unicode notice preserved as one frame");
                a.out.println(Protocol.PREFIX_MESSAGE+Protocol.encode("Question | line one\nline two"));
                check(Protocol.decode(a.in.readLine().substring(Protocol.PREFIX_MESSAGE.length())).equals("Server received: Question | line one\nline two"),"Reply acknowledgement and framing");
                NetNoticeServer.startPoll("A | B?",Arrays.asList("One | Two","Three"));
                String[] fields=a.in.readLine().substring(Protocol.PREFIX_POLL.length()).split("\\|");
                check(fields.length==3 && Protocol.decode(fields[0]).equals("A | B?") && Protocol.decode(fields[1]).equals("One | Two"),"Pipe characters preserved in poll fields");
                a.out.println("ANSWER|99"); check(a.in.readLine().startsWith("ERROR|"),"Out-of-range vote rejected");
                a.out.println("ANSWER|0"); check(a.in.readLine().equals("POLL_RESULT|1|0"),"Vote counted");
                a.out.println("ANSWER|1"); check(a.in.readLine().equals("POLL_RESULT|0|1"),"Vote change counted once");
                NetNoticeServer.endPoll(); check(a.in.readLine().equals("POLL_END|0|1"),"Final counts delivered");
                NetNoticeServer.startPoll("[Quiz] Choose an answer",Arrays.asList("A","B")); check(a.in.readLine().startsWith("POLL|"),"Quiz delivered");
                a.out.println("ANSWER|0");
                a.socket.setSoTimeout(300);
                boolean hidden=false; try { a.in.readLine(); } catch(SocketTimeoutException expected) {hidden=true;}
                check(hidden,"Quiz counts withheld on the network");
                a.socket.setSoTimeout(2000);
                try(Peer late=new Peer(port,"Late")) {
                    check(late.in.readLine().equals("JOINED|Late"),"Late student joins quiz");
                    check(late.in.readLine().startsWith("POLL|"),"Late student receives quiz");
                    late.socket.setSoTimeout(300); boolean privateCounts=false;
                    try { late.in.readLine(); } catch(SocketTimeoutException expected) {privateCounts=true;}
                    check(privateCounts,"Late join does not leak quiz counts");
                    NetNoticeServer.endPoll();
                    check(a.in.readLine().equals("POLL_END|1|0") && late.in.readLine().equals("POLL_END|1|0"),"Quiz results revealed at close");
                }
                a.out.println("ANSWER|0"); check(a.in.readLine().startsWith("ERROR|"),"Votes rejected after close");
                boolean rejected=false;
                try { NetNoticeServer.startPoll("Invalid",Collections.singletonList("Only one")); }catch(IllegalArgumentException expected){rejected=true;}
                check(rejected,"Invalid poll rejected before broadcast");
            }
            NetNoticeServer.stopServer();
            for(int i=0;i<3;i++) {
                NetNoticeServer.startServer(port);
                try(Peer peer=new Peer(port,"Restart"+i)) { check(peer.in.readLine().startsWith("JOINED|"),"Server restart "+(i+1)); }
                NetNoticeServer.stopServer();
            }
            byte[] muted=UITheme.Sound.samples(0),normal=UITheme.Sound.samples(35);
            boolean nonzero=false; for(byte b:muted) checkZero(b); for(byte b:normal) if(b!=0)nonzero=true;
            check(nonzero && normal.length==57330,"Soft bell has 650 ms of audio and mute generates silence");
            Path export=Files.createTempFile("netnotice-export-",".txt");
            try { UITheme.writeText(export,"café\nසිංහල"); check(Files.readString(export).equals("café\nසිංහල"),"UTF-8 text export"); } finally {Files.deleteIfExists(export);}
            Files.createDirectories(Paths.get("out","previews"));
            javax.sound.sampled.AudioFormat format=new javax.sound.sampled.AudioFormat(44100,16,1,true,false);
            try(javax.sound.sampled.AudioInputStream audio=new javax.sound.sampled.AudioInputStream(new ByteArrayInputStream(normal),format,normal.length/2)) {
                javax.sound.sampled.AudioSystem.write(audio,javax.sound.sampled.AudioFileFormat.Type.WAVE,Paths.get("out","previews","soft-bell.wav").toFile());
            }
            System.out.println("ALL FUNCTIONAL REGRESSIONS PASSED");
        } finally { NetNoticeServer.stopServer(); }
    }
    static void checkZero(byte value) {if(value!=0) throw new AssertionError("Mute is not silent");}
}
