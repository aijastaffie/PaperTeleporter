package com.paperteleporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

final class DataFileBackup {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private DataFileBackup() {
    }

    static void restorePrimaryIfMissing(Path primaryPath, Path backupCurrentPath, Logger logger) {
        try {
            if (Files.exists(primaryPath) || Files.notExists(backupCurrentPath)) {
                return;
            }
            Files.createDirectories(primaryPath.getParent());
            Files.copy(backupCurrentPath, primaryPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Restored missing data file from backup: " + primaryPath.getFileName());
        } catch (IOException e) {
            logger.warning("Failed to restore backup for " + primaryPath.getFileName() + ": " + e.getMessage());
        }
    }

    static void writeBackups(Path primaryPath, Path backupCurrentPath, Path backupSnapshotsDir, Logger logger) {
        if (Files.notExists(primaryPath)) {
            return;
        }
        try {
            Files.createDirectories(backupCurrentPath.getParent());
            Files.copy(primaryPath, backupCurrentPath, StandardCopyOption.REPLACE_EXISTING);

            Files.createDirectories(backupSnapshotsDir);
            String baseName = primaryPath.getFileName().toString();
            int dot = baseName.lastIndexOf('.');
            String prefix = dot > 0 ? baseName.substring(0, dot) : baseName;
            String ext = dot > 0 ? baseName.substring(dot) : "";
            String snapshotName = prefix + "-" + LocalDateTime.now().format(TS) + ext;
            Path snapshotPath = backupSnapshotsDir.resolve(snapshotName);
            Files.copy(primaryPath, snapshotPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warning("Failed to write backup for " + primaryPath.getFileName() + ": " + e.getMessage());
        }
    }
}
