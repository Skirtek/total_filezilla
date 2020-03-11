import javafx.scene.control.Label;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.text.DecimalFormat;

public class DiskHelper {
    private final DecimalFormat gigabytesTextFormat = new DecimalFormat("#.##");

    public Label getDiskNameLabel(File disk) {
        String diskName = FileSystemView.getFileSystemView().getSystemDisplayName(disk);
        return new Label(diskName);
    }

    public Label getSpaceDescLabel(long freeSpace, long totalSpace) {
        return new Label(formatSpaceToGigabytes(freeSpace) + " GB wolnych z " + formatSpaceToGigabytes(totalSpace) + " GB");
    }

    public double getUsedDiskSpaceInPercents(long freeSpace, long totalSpace) {
        return ((totalSpace - freeSpace) / (double) totalSpace);
    }

    private String formatSpaceToGigabytes(long bytesCount) {
        return gigabytesTextFormat.format(bytesCount / (Math.pow(1024, 3)));
    }
}
