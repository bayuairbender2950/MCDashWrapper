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

public class ForgeInstaller implements VersionInstaller {

    private static final Logger LOG = new Logger(ForgeInstaller.class);
    private static final OkHttpClient client = new OkHttpClient();
    private static final String FORGE_JSON_URL = "https://raw.githubusercontent.com/liebki/MinecraftServerForkDownloads/refs/heads/main/forge_downloads.json";
    private static Map<String, String> forgeVersions;

    static {
        try {
            Request request = new Request.Builder().url(FORGE_JSON_URL).build();
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                forgeVersions = new JSONObject(json).toMap();
            }
            response.close();
        } catch (Exception e) {
            LOG.error("Failed to fetch Forge versions", e);
        }
    }

    @Override
    public boolean isValidVersion(String software, String version) {
        return forgeVersions != null && forgeVersions.containsKey(version);
    }

    @Override
    public boolean installVersion(String software, String version) {
        if (!isValidVersion(software, version)) {
            LOG.error("Invalid Forge version: " + version);
            return false;
        }

        try {
            File file = new File(ServerVersionManager.getVersionFolder(), software + "-" + version + ".jar");
            if (file.exists()) {
                LOG.info("Forge version " + version + " is already installed.");
                return true;
            }

            String downloadUrl = forgeVersions.get(version);
            Request request = new Request.Builder().url(downloadUrl).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                LOG.error("Failed to download Forge version: " + version);
                return false;
            }

            try (InputStream inputStream = response.body().byteStream()) {
                Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            LOG.info("Forge version " + version + " has been installed.");
            return true;
        } catch (Exception e) {
            LOG.error("An error occurred while installing Forge version: " + version, e);
            return false;
        }
    }
}
