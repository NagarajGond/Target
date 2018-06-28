import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class CopyFileAndFolderHealthMonitor {

    private static final int COPY_WINDOW = 2; //2 minutes
    private static final int MONITORING_WINDOW = 5; //5 minutes
    private static final int APP_SHUTDOWN_WINDOW = 24; //24 hours - assumption

    private static final String FILE_SOURCE = "G:\\temp"; //Modify the folder location before running
    private static final String FILE_DEST = "G:\\secured";
    private static final String FILE_ARC = "G:\\archived";

    private static Path tempDir = Paths.get(FILE_SOURCE);
    private static Path securedDir = Paths.get(FILE_DEST);
    private static Path archivedDir = Paths.get(FILE_ARC);


    public static void main(String[] arggs) {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        final ScheduledFuture<?> scheduledHandler = service.scheduleAtFixedRate(new FileCopyTask(), 0, COPY_WINDOW, TimeUnit.MINUTES);
        final ScheduledFuture<?> monitorHandler = service.scheduleAtFixedRate(new FolderMonitor(), COPY_WINDOW, MONITORING_WINDOW, TimeUnit.MINUTES);

        //to terminate the scheduler threads
        service.schedule(new Runnable() {
            @Override
            public void run() {
                scheduledHandler.cancel(true);
                monitorHandler.cancel(true);
                service.shutdown();
            }
        }, APP_SHUTDOWN_WINDOW, TimeUnit.HOURS);
    }

    static class FileCopyTask implements Runnable {

        public void run() {
            File files = new File(tempDir.toString());
            File[] list = files.listFiles();

            if(list.length <= 0) {
                System.out.println("No files to copy...");
                return;
            }
            System.out.println("Copying files in every 2 mins...");
            try {
                for (File file : list){
                    File source = new File(tempDir + "\\" + file.getName());
                    File destination = new File(securedDir + "\\" + file.getName());
                    Files.copy(source.toPath(), destination.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }

            } catch (IOException io) {
                io.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class FolderMonitor implements Runnable {

        @Override
        public void run() {
            System.out.println("Monitoring folder in every 5 mins...");
            try {
                long foldersize = getCurrentFolderSize();
                if(foldersize <= 0) {
                    System.out.println("Current secured folder size is : 0");
                    return;
                }
                double s = foldersize/1024;
                if(s < 1024) {
                    System.out.println("Current secured folder size is : " + String.format("%.2f", s) + " KB");
                } else {
                    System.out.println("Current secured folder size is : " + String.format("%.2f", s/1024) + " MB");
                }
                if(s/1024 >= 100) {
                    archiveOldFiles();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void archiveOldFiles() {
            System.out.println("Archiving older files...");
            long size = 0;
            int count = 0;
            File files = new File(securedDir.toString());
            File[] list = files.listFiles();
            List<File> archiveFiles = new ArrayList<File>();
            Arrays.sort(list, (file1, file2) -> Long.valueOf(file2.lastModified()).compareTo(Long.valueOf(file1.lastModified())));

            if (list.length > 0) {
                for (File file : list) {
                    size += file.length();
                    if (size/1024 > 102400) {
                        archiveFiles.add(file);
                    }
                }
                try {
                    for (File archfile : archiveFiles){
                        File source = new File(securedDir + "\\" + archfile.getName());
                        File destination = new File(archivedDir + "\\" + archfile.getName());
                        Files.move(source.toPath(), destination.toPath(),
                                StandardCopyOption.ATOMIC_MOVE);
                        count++;
                    }
                    System.out.println("Total count of archived files: " + count);
                    long folderSize = getCurrentFolderSize();
                    if(folderSize <= 0) {
                        System.out.println("Current secured folder size after archiving is : 0");
                        return;
                    }
                    double s = folderSize/1024;
                    if(s < 1024) {
                        System.out.println("Current secured folder size after archiving is : " + String.format("%.2f", s) + " KB");
                    } else {
                        System.out.println("Current secured folder size after archiving is : " + String.format("%.2f", s/1024) + " MB");
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private long getCurrentFolderSize(){
            File files = new File(securedDir.toString());
            File[] list = files.listFiles();
            long size = 0;
            if(list.length > 0 ) {
                for(File file : list) {
                    if(file.toString().contains(".bat") || file.toString().contains(".sh")) {// || file.toString().contains(".exe")) {
                        file.delete();
                        System.out.println("File deleted : " + file.getName());
                    }
                    if(file.isFile()) {
                        size += file.length();
                    }
                }
            }
            return size;
        }
    }
}
