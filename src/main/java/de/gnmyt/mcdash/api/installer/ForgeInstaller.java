package de.gnmyt.mcdash.api.installer;

import com.google.gson.Gson;
import de.gnmyt.mcdash.api.Logger;
import de.gnmyt.mcdash.api.ServerVersionManager;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ForgeInstaller implements VersionInstaller {
    private static final Logger LOG = new Logger(ForgeInstaller.class);
    private static final OkHttpClient client = new OkHttpClient();
    private static final String FORGE_VERSION_API = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";

    @Override
    public boolean isValidVersion(String serverType, String version) {
        try {
            Request request = new Request.Builder()
                .url(FORGE_VERSION_API)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                return json.has(version);
            }
        } catch (Exception e) {
            LOG.error("Failed to validate Forge version", e);
            return false;
        }
    }

    @Override
    public boolean installVersion(String serverType, String version) {
        try {
            String installerUrl = String.format(
                "https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar",
                version, version
            );

            File installerFile = new File(ServerVersionManager.getVersionFolder(), "forge-installer.jar");
            if (!downloadFile(installerUrl, installerFile)) return false;

            // Execute installer
            Process process = Runtime.getRuntime().exec(
                "java -jar " + installerFile.getAbsolutePath() + " --installServer"
            );
            
            return process.waitFor() == 0;
        } catch (Exception e) {
            LOG.error("Forge installation failed", e);
            return false;
        }
    }
}