package netnotice.test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import netnotice.client.ClientGUI;
import netnotice.server.*;
import netnotice.common.*;

/** Exercises real GUI actions over TCP and renders every page at two window sizes. */
public class DesktopWorkflowTest {
    static Object field(Object o,String name) throws Exception { Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o); }
    static void call(Object o,String name,Class<?>[] types,Object...args) throws Exception { Method m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(o,args); }
    interface Action {void run() throws Exception;}
    static void edt(Action action)throws Exception { SwingUtilities.invokeAndWait(()->{try {action.run();}catch(Exception ex){throw new RuntimeException(ex);}}); }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);System.out.println("PASS: "+message);}
    interface Condition {boolean ready() throws Exception;}
    static void until(Condition condition,String message)throws Exception {
        long deadline=System.currentTimeMillis()+4000;
        while(System.currentTimeMillis()<deadline){final boolean[] done={false};edt(()->done[0]=condition.ready());if(done[0]){check(true,message);return;}Thread.sleep(25);}
        throw new AssertionError("Timed out: "+message);
    }
    static void click(Object o,String name)throws Exception{edt(()->((JButton)field(o,name)).doClick());}
    static void layout(Component c) {
        if(c instanceof Container){((Container)c).invalidate(); for(Component child:((Container)c).getComponents())layout(child);}
    }
    static void validateFields(Component c) {
        if(c instanceof JTextField && c.isVisible()) {
            check(c.getHeight()>=24,"Input has usable height");
            check(c.getX()>=0 && c.getX()+c.getWidth()<=c.getParent().getWidth()+1,"Input fits parent width");
        }
        if(c instanceof Container) for(Component child:((Container)c).getComponents()) if(child.isVisible())validateFields(child);
    }
    static JTabbedPane tabs(Component c){if(c instanceof JTabbedPane)return(JTabbedPane)c;if(c instanceof Container)for(Component child:((Container)c).getComponents()){JTabbedPane found=tabs(child);if(found!=null)return found;}return null;}
    static void render(JFrame frame,String name)throws Exception {
        frame.addNotify(); JTabbedPane pages=tabs(frame);
        for(int width:new int[]{1100,600}) {
            frame.setSize(width,width==600?600:780);
            for(int i=0;i<pages.getTabCount();i++) {
                pages.setSelectedIndex(i);
                for(int n=0;n<4;n++){layout(frame);frame.validate();}
                validateFields(pages.getComponentAt(i));
                BufferedImage image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics g=image.getGraphics();frame.getRootPane().printAll(g);g.dispose();
                ImageIO.write(image,"png",Paths.get("out","previews",name+"-"+width+"-"+i+".png").toFile());
            }
        }
    }
    static <T> T find(Component root,Class<T> type) {
        if(type.isInstance(root))return type.cast(root);
        if(root instanceof Container)for(Component child:((Container)root).getComponents()){T match=find(child,type);if(match!=null)return match;}
        return null;
    }
    static JButton button(Component root,String title) {
        if(root instanceof JButton && title.equals(((JButton)root).getText()))return(JButton)root;
        if(root instanceof Container)for(Component child:((Container)root).getComponents()){JButton match=button(child,title);if(match!=null)return match;}
        return null;
    }
    static JDialog dialog(String title) {
        for(Window window:Window.getWindows())if(window instanceof JDialog && title.equals(((JDialog)window).getTitle()))return(JDialog)window;
        return null;
    }
    static void export(JButton source,Path path)throws Exception {
        edt(()->{
            Timer choose=new Timer(150,e->{
                for(Window window:Window.getWindows())if(window.isVisible()){
                    JFileChooser chooser=find(window,JFileChooser.class);
                    if(chooser!=null){chooser.setSelectedFile(path.toFile());chooser.approveSelection();break;}
                }
            });choose.setRepeats(false);choose.start();source.doClick();
        });
        check(Files.exists(path),"Export dialog writes file");
    }
    public static void main(String[] args)throws Exception {
        int port;try(ServerSocket socket=new ServerSocket(0)){port=socket.getLocalPort();}
        Files.createDirectories(Paths.get("out","previews"));
        Preferences clientPrefs=Preferences.userNodeForPackage(ClientGUI.class),serverPrefs=Preferences.userNodeForPackage(ServerGUI.class);
        String oldName=clientPrefs.get("name",null),oldHost=clientPrefs.get("host",null),oldPort=clientPrefs.get("port",null),oldDraft=serverPrefs.get("draft",null);
        boolean sound=UITheme.Sound.isSoundEnabled(); UITheme.Sound.setSoundEnabled(false);
        ServerGUI[] s={null};ClientGUI[] c={null};
        try {
            edt(()->{s[0]=new ServerGUI();c[0]=new ClientGUI();
                if(System.getProperty("os.name").startsWith("Windows"))check(UITheme.FONT_FAMILY.toLowerCase().contains("inter"),"Bundled Inter font loaded");((JTextField)field(s[0],"portField")).setText(String.valueOf(port));});
            edt(() -> {
                c[0].setSize(760, 540);
                c[0].addNotify();
                c[0].validate();
                Component welcome = (Component)field(c[0], "welcome");
                check(welcome.isVisible() && !((Component)field(c[0], "notice")).isVisible(), "Offline classroom shows join invitation");
                button(welcome, "Join classroom").doClick();
                check(tabs(c[0]).getSelectedIndex() == 2, "Join invitation opens connection form");
                JToggleButton firstPage = find(c[0], JToggleButton.class);
                firstPage.doClick();
                check(tabs(c[0]).getSelectedIndex() == 0, "Compact page strip opens Classroom");
            });
            click(s[0],"startStopButton");check(NetNoticeServer.isRunning(),"Server starts from button");
            edt(()->{((JTextField)field(c[0],"nameField")).setText("UI Student");((JTextField)field(c[0],"ipField")).setText("127.0.0.1");((JTextField)field(c[0],"portField")).setText(String.valueOf(port));});
            click(c[0],"connectButton");until(()->(boolean)field(c[0],"isConnected"),"Client connects from button");
            until(()->((DefaultListModel<?>)field(s[0],"clientListModel")).size()==1,"Student appears in roster");
            check(clientPrefs.get("name","").equals("UI Student") && clientPrefs.get("port","").equals(String.valueOf(port)),"Successful connection details saved");
            edt(()->((JTextField)field(s[0],"searchClientField")).setText("missing"));
            edt(()->check(((DefaultListModel<?>)field(s[0],"clientListModel")).isEmpty(),"Roster search filters"));
            edt(()->((JTextField)field(s[0],"searchClientField")).setText(""));
            edt(()->{
                ((JTextArea)field(s[0],"announcementArea")).setText("Recover this draft");
                ServerGUI reopened=new ServerGUI();
                check(((JTextArea)field(reopened,"announcementArea")).getText().equals("Recover this draft"),"Draft recovered in reopened workspace");
                reopened.dispose();NetNoticeServer.setListener(s[0]);
            });
            edt(() -> check(((Component)field(c[0], "welcome")).isVisible() && ((Component)field(c[0], "reply")).isVisible(), "Connected classroom accepts questions before first notice"));
            String notice="A fresh start.\nToday's workshop begins at 10:00. Bring your ideas | café.";
            edt(()->((JTextArea)field(s[0],"announcementArea")).setText(notice));click(s[0],"sendAnnouncementButton");
            until(()->((JTextArea)field(c[0],"announcementDisplay")).getText().equals(notice),"Multiline notice reaches client GUI");
            edt(()->check(((java.util.List<?>)field(c[0],"notices")).size()==1,"Notice retained in history"));
            click(c[0],"markReadButton");edt(()->check(!((JButton)field(c[0],"markReadButton")).isEnabled(),"Mark as read updates state"));
            click(c[0],"copyNoticeButton");
            check(java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor).equals(notice),"Copy notice writes clipboard");
            edt(()->((JTextField)field(c[0],"replyField")).setText("Could you explain the next step?"));click(c[0],"sendReplyButton");
            until(()->((JTextArea)field(s[0],"logArea")).getText().contains("Could you explain"),"Student reply reaches instructor log");
            until(()->field(s[0],"messages").toString().contains("UI Student") && field(s[0],"messages").toString().contains("Could you explain"),"Student message appears in main-screen inbox");
            edt(()->{
                check((int)field(s[0],"unreadMessageCount")==1,"Unread message counted");
                check(((NotificationCenter)field(s[0],"notifications")).deliveredCount()==1,"Instructor notification requested");
                check(((NotificationCenter)field(c[0],"notifications")).deliveredCount()==1,"Student announcement notification requested");
                call(s[0],"openMessages",new Class[]{});
                check((int)field(s[0],"unreadMessageCount")==0 && tabs(s[0]).getSelectedIndex()==0,"View opens inbox and clears unread state");
                s[0].setVisible(true);
            });
            edt(()->call(s[0],"onStudentMessage",new Class[]{String.class,String.class},"Avery","Could you explain the next step?"));
            until(()->((NotificationCenter)field(s[0],"notifications")).activeCount()==2,"Both replies retained in toolbar");
            edt(()->{
                NotificationCenter center=(NotificationCenter)field(s[0],"notifications");
                check(center.isShowing() && SwingUtilities.getWindowAncestor(center)==s[0],"Notification is inside instructor frame");
                for(Window child:s[0].getOwnedWindows()) check(!(child instanceof JWindow && child.isShowing()),"No external notification window");
                center.toolbarButton().doClick();
                check(!center.isVisible() && center.activeCount()==2,"Bell collapses without losing notifications");
                center.toolbarButton().doClick();
                button(center,"View").doClick();
                check(center.activeCount()==1 && (int)field(s[0],"unreadMessageCount")==0,"View returns to instructor inbox");
                button(center,"Dismiss").doClick();
                check(center.activeCount()==0 && !center.isVisible(),"Dismiss clears final toolbar alert");
                s[0].setState(Frame.ICONIFIED);
                s[0].onStudentMessage("Avery","Can we review the last example?");
            });
            until(()->((NotificationCenter)field(s[0],"notifications")).activeCount()==1,"Message retained in minimized toolbar");
            edt(()->{
                NotificationCenter center=(NotificationCenter)field(s[0],"notifications");
                check(center.activeCount()==1 && s[0].getState()==Frame.ICONIFIED,"Arrival leaves minimized frame minimized");
                s[0].setState(Frame.NORMAL);
                s[0].validate();
                s[0].setVisible(false);
            });
            for(boolean quiz:new boolean[]{false,true}) {
                edt(()->{((JTextField)field(s[0],"pollQuestionField")).setText(quiz?"Choose the correct answer":"What should we explore next?");JTextField[] choices=(JTextField[])field(s[0],"pollOptionFields");choices[0].setText("Design | systems");choices[1].setText("Networking");((JCheckBox)field(s[0],"quizModeCheckbox")).setSelected(quiz);});
                click(s[0],"sendPollButton");until(()->field(c[0],"currentOptions")!=null,"Activity appears on client");
                edt(()->((JButton[])field(c[0],"pollOptionButtons"))[0].doClick());
                until(()->((JTextArea)field(s[0],"pollResultsArea")).getText().contains(quiz ? "Quiz results · Live · 1 answer" : "Poll results · Live · 1 vote"),"Instructor sees the response for this activity");
                if(quiz)edt(()->check(!((UITheme.PollProgressBar[])field(c[0],"visualBars"))[0].isVisible(),"Quiz results hidden in GUI"));
                else until(()->((JTextArea)field(c[0],"pollResultsArea")).getText().contains("Poll results · Live · 1 vote"),"Live results appear in GUI");
                click(s[0],"endPollButton");until(()->!((JButton[])field(c[0],"pollOptionButtons"))[0].isEnabled(),"Activity closes on client");
                edt(()->{
                    check(((UITheme.PollProgressBar[])field(c[0],"visualBars"))[0].isVisible(),"Final results visible");
                    String tally = ((JTextArea)field(c[0],"pollResultsArea")).getText();
                    check(tally.contains(quiz ? "Quiz results · Final · 1 answer" : "Poll results · Final · 1 vote"),"Final tally matches activity type");
                    check(tally.contains(quiz ? "Your answer" : "Your vote"),"Selected choice uses activity terminology");
                    if (quiz) check(!tally.contains("vote"),"Quiz results use answer terminology throughout");
                });
            }
            Path exportFolder=Files.createTempDirectory("netnotice-ui-exports-");
            try {
                JButton[] exportLog={null},exportResults={null};
                edt(()->{exportLog[0]=button(s[0],"Export log");exportResults[0]=button(s[0],"Export results");});
                export(button(s[0],"Export messages"),exportFolder.resolve("messages.txt"));
                check(Files.readString(exportFolder.resolve("messages.txt")).contains("Could you explain"),"Inbox export contains student message");
                export(exportLog[0],exportFolder.resolve("activity.txt"));
                check(Files.readString(exportFolder.resolve("activity.txt")).contains("Could you explain"),"Activity export contains student reply");
                export(exportResults[0],exportFolder.resolve("results.txt"));
                check(Files.readString(exportFolder.resolve("results.txt")).contains("Quiz results · Final · 1 answer"),"Results export contains final tally");
                edt(()->button(c[0],"History").doClick());
                JDialog history=dialog("Announcement history");check(history!=null,"History opens");
                edt(()->{JTextField search=find(history,JTextField.class);search.setText("missing");check(find(history,JTextArea.class).getText().equals("No announcements found."),"History search empty state");search.setText("fresh");check(find(history,JTextArea.class).getText().contains("fresh"),"History search finds matching notice");});
                export(button(history,"Export history"),exportFolder.resolve("history.txt"));
                check(Files.readString(exportFolder.resolve("history.txt")).contains(notice),"History export preserves notice");edt(()->history.dispose());
                edt(()->{
                    Timer close=new Timer(200,e->{JDialog preview=dialog("Announcement preview");if(preview!=null)preview.dispose();});close.setRepeats(false);close.start();button(s[0],"Preview").doClick();
                    check(dialog("Announcement preview")!=null,"Announcement preview opens");
                });
                int oldVolume=UITheme.Sound.getVolume();
                edt(()->{
                    Timer configure=new Timer(200,e->{JDialog settings=dialog("Notification sound");if(settings!=null){find(settings,JSlider.class).setValue(42);settings.dispose();}});configure.setRepeats(false);configure.start();button(c[0],"Sound settings").doClick();
                    check(UITheme.Sound.getVolume()==42,"Sound settings update volume");UITheme.Sound.setVolume(oldVolume);
                });
                edt(()->{button(s[0],"Clear draft").doClick();check(((JTextArea)field(s[0],"announcementArea")).getText().isEmpty(),"Clear draft resets composer");});
            } finally { try(java.util.stream.Stream<Path> files=Files.list(exportFolder)){for(Path path:files.toList())Files.deleteIfExists(path);}Files.deleteIfExists(exportFolder); }
            for(int i=0;i<3;i++) {
                click(c[0],"disconnectButton");until(()->!(boolean)field(c[0],"isConnected"),"Client disconnects");
                until(()->((DefaultListModel<?>)field(s[0],"clientListModel")).isEmpty(),"Disconnect cleans roster");
                click(c[0],"connectButton");until(()->(boolean)field(c[0],"isConnected"),"Client reconnects");
            }
            edt(()->{render(s[0],"instructor");render(c[0],"student");});
            click(s[0],"startStopButton");until(()->!(boolean)field(c[0],"isConnected"),"Server stop disconnects client");
            System.out.println("ALL DESKTOP WORKFLOWS PASSED");
        } finally {
            NetNoticeServer.stopServer();
            edt(()->{if(c[0]!=null)c[0].dispose();if(s[0]!=null)s[0].dispose();});
            restore(clientPrefs,"name",oldName);restore(clientPrefs,"host",oldHost);restore(clientPrefs,"port",oldPort);restore(serverPrefs,"draft",oldDraft);
            UITheme.Sound.setSoundEnabled(sound);NetNoticeServer.setListener(null);
        }
        System.exit(0);
    }
    static void restore(Preferences prefs,String key,String value){if(value==null)prefs.remove(key);else prefs.put(key,value);}
}

