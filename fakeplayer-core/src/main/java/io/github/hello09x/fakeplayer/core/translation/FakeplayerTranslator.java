package io.github.hello09x.fakeplayer.core.translation;

import io.github.hello09x.devtools.core.translation.TranslationConfig;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translator backed by the plugin's {@code .properties} resource bundles.
 * <p>
 * Replaces devtools' {@code PluginTranslator}, which depends on {@code TranslationRegistry} and
 * {@code UTF8ResourceBundleControl}. Both were removed in Adventure 5 (Paper 26.2+), while
 * {@code TranslationStore} does not exist before Adventure 4.20 (Paper 1.21.4). This class
 * only uses the {@link Translator} interface, which is available in every supported version.
 */
public class FakeplayerTranslator implements Translator {

    private final static ResourceBundle.Control UTF8_CONTROL = new UTF8Control();

    private final Plugin plugin;

    private final TranslationConfig config;

    private final Key name;

    private final Map<Locale, Map<String, MessageFormat>> translations = new ConcurrentHashMap<>();

    public FakeplayerTranslator(@NotNull Plugin plugin, @NotNull TranslationConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.name = Key.key(plugin.getName().toLowerCase(Locale.ROOT), "translator");
    }

    @Override
    public @NotNull Key name() {
        return this.name;
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @Nullable Locale locale) {
        var defaultLocale = config.defaultLocale();
        if (locale == null) {
            locale = defaultLocale;
        }

        var format = this.getTranslations(locale).get(key);
        if (format == null && !locale.equals(defaultLocale)) {
            format = this.getTranslations(defaultLocale).get(key);
        }
        return format;
    }

    /**
     * Drops every loaded translation so that they are read again from disk on next use
     */
    public void reload() {
        this.translations.clear();
    }

    private @NotNull Map<String, MessageFormat> getTranslations(@NotNull Locale locale) {
        return this.translations.computeIfAbsent(locale, this::load);
    }

    private @NotNull Map<String, MessageFormat> load(@NotNull Locale locale) {
        // Files in the data folder take precedence over the ones bundled in the jar
        try (var dataFolderClassLoader = new URLClassLoader(new URL[]{plugin.getDataFolder().toURI().toURL()}, null)) {
            for (var classLoader : new ClassLoader[]{dataFolderClassLoader, plugin.getClass().getClassLoader()}) {
                ResourceBundle bundle;
                try {
                    bundle = ResourceBundle.getBundle(config.baseName(), locale, classLoader, UTF8_CONTROL);
                } catch (MissingResourceException e) {
                    continue;
                }

                var formats = new HashMap<String, MessageFormat>();
                for (var key : bundle.keySet()) {
                    formats.put(key, new MessageFormat(bundle.getString(key), locale));
                }
                return formats;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load translations for locale %s: %s".formatted(locale, e.getMessage()));
        }

        return Collections.emptyMap();
    }

    /**
     * Reads {@code .properties} bundles as UTF-8 and never caches them, so that {@link #reload()} picks up changes.
     */
    private final static class UTF8Control extends ResourceBundle.Control {

        @Override
        public @Nullable ResourceBundle newBundle(
                @NotNull String baseName,
                @NotNull Locale locale,
                @NotNull String format,
                @NotNull ClassLoader loader,
                boolean reload
        ) throws IllegalAccessException, InstantiationException, IOException {
            if (!format.equals("java.properties")) {
                return super.newBundle(baseName, locale, format, loader, reload);
            }

            var resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            var url = loader.getResource(resourceName);
            if (url == null) {
                return null;
            }

            var connection = url.openConnection();
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()) {
                return new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }

        @Override
        public long getTimeToLive(@NotNull String baseName, @NotNull Locale locale) {
            return TTL_DONT_CACHE;
        }

    }

}
