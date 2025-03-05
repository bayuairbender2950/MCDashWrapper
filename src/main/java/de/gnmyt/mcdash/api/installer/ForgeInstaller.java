package de.gnmyt.mcdash.api.installer;

import de.gnmyt.mcdash.api.Logger;
import de.gnmyt.mcdash.api.ServerVersionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ForgeInstaller implements VersionInstaller {

    private static final Logger LOG = new Logger(ForgeInstaller.class);
    private static final OkHttpClient client = new OkHttpClient();
    private static final String FORGE_JSON_URL = "https://raw.githubusercontent.com/liebki/MinecraftServerForkDownloads/refs/heads/main/forge_downloads.json";
    private static Map<String, String> forgeVersions = null;

    @Override
    public boolean isValidVersion(String software, String version) {
        if (forgeVersions == null) {
            fetchForgeVersions();
        }
        return forgeVersions != null && forgeVersions.containsKey(version);
    }

    private synchronized void fetchForgeVersions() {
        if (forgeVersions != null) return;

        try {
            Request request = new Request.Builder().url(FORGE_JSON_URL).build();
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                forgeVersions = new JSONObject(json).toMap();
                LOG.info("Successfully fetched Forge versions");
            } else {
                LOG.error("Failed to fetch Forge versions: HTTP " + response.code());
            }
            response.close();
        } catch (Exception e) {
            LOG.error("Failed to fetch Forge versions", e);
        }
    }

    @Override
    public boolean installVersion(String software, String version) {
        if (!isValidVersion(software, version)) {
            LOG.error("Invalid Forge version: " + version);
            return false;
        }

        try {
            String downloadUrl = forgeVersions.get(version);

            // Extract full Forge version from the installer URL
            String[] urlParts = downloadUrl.split("/");
            String installerJarName = urlParts[urlParts.length - 1];
            Matcher matcher = Pattern.compile("forge-(.+?)-installer\\.jar").matcher(installerJarName);
            if (!matcher.find()) {
                LOG.error("Invalid Forge installer JAR name: " + installerJarName);
                return false;
            }
            String fullVersion = matcher.group(1);
            String targetFilename = software + "-" + fullVersion + ".jar";
            File targetFile = new File(ServerVersionManager.getVersionFolder(), targetFilename);

            if (targetFile.exists()) {
                LOG.info("Forge version " + fullVersion + " is already installed.");
                return true;
            }

            File installerFile = File.createTempFile("forge-installer-", ".jar");
            installerFile.deleteOnExit();

            // Download installer
            Request request = new Request.Builder().url(downloadUrl).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                LOG.error("Failed to download Forge installer for version: " + version);
                return false;
            }

            try (InputStream inputStream = response.body().byteStream()) {
                Files.copy(inputStream, installerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Execute installer
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java", "-jar", installerFile.getAbsolutePath(), "--installServer"
            );
            processBuilder.directory(ServerVersionManager.getVersionFolder());
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                LOG.error("Forge installer exited with code " + exitCode);
                return false;
            }

            // Verify generated server JAR
            File generatedJar = new File(ServerVersionManager.getVersionFolder(), "forge-" + fullVersion + ".jar");
            if (!generatedJar.exists()) {
                LOG.error("Forge installer did not generate server JAR: " + generatedJar.getName());
                return false;
            }

            Files.move(generatedJar.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(installerFile.toPath());

            LOG.info("Forge version " + fullVersion + " installed successfully.");
            return true;
        } catch (Exception e) {
            LOG.error("Error installing Forge version " + version, e);
            return false;
        }
    }
}