package me.rrs.headdrop.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import me.rrs.headdrop.HeadDrop;
import me.rrs.headdrop.database.EntityHead;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HolidayManager {
    private final HeadDrop plugin;
    private YamlDocument holidaysConfig;
    private final Map<String, YamlDocument> holidayConfigs = new HashMap<>();
    private String currentActiveHoliday = null;
    private LocalDate lastCheckedDate = null;
    private String forcedHoliday = null;  // For testing/debugging

    public HolidayManager(HeadDrop plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[Holiday] Initializing Holiday Manager...");
        loadHolidaysConfig();
        extractHolidayConfigs();
        checkActiveHoliday();

        // Log startup status
        if (holidaysConfig != null && holidaysConfig.getBoolean("Holidays.Enable", false)) {
            if (currentActiveHoliday != null) {
                plugin.getLogger().info("[Holiday] ✓ Active Holiday: " + currentActiveHoliday);
            } else {
                plugin.getLogger().info("[Holiday] No active holidays for current date: " + LocalDate.now());
            }
        } else {
            plugin.getLogger().info("[Holiday] Holiday system is disabled in config");
        }
    }

    private void loadHolidaysConfig() {
        try {
            File holidaysFile = new File(plugin.getDataFolder(), "holidays.yml");
            if (!holidaysFile.exists()) {
                plugin.saveResource("holidays.yml", false);
            }
            holidaysConfig = YamlDocument.create(holidaysFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load holidays.yml: " + e.getMessage());
        }
    }

    /**
     * Extracts all holiday config files from the JAR on plugin startup
     * This ensures config files are visible to server admins
     */
    private void extractHolidayConfigs() {
        if (holidaysConfig == null) {
            return;
        }

        // Create holidays directory if it doesn't exist
        File holidaysDir = new File(plugin.getDataFolder(), "holidays");
        if (!holidaysDir.exists()) {
            holidaysDir.mkdirs();
        }

        // Get the list of holidays from config
        List<String> holidays = holidaysConfig.getStringList("Holidays.List");

        for (String holidayName : holidays) {
            String fileName = "holidays/" + holidayName.toLowerCase() + ".yml";
            File holidayFile = new File(plugin.getDataFolder(), fileName);

            // Only extract if file doesn't exist (don't overwrite customizations)
            if (!holidayFile.exists()) {
                try {
                    plugin.saveResource(fileName, false);
                    if (plugin.getConfiguration().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Extracted holiday config: " + fileName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not extract holiday config " + fileName + ": " + e.getMessage());
                }
            }
        }
    }

    private YamlDocument loadHolidayConfig(String holidayName) {
        if (holidayConfigs.containsKey(holidayName)) {
            return holidayConfigs.get(holidayName);
        }

        try {
            String fileName = "holidays/" + holidayName.toLowerCase() + ".yml";
            File holidayFile = new File(plugin.getDataFolder(), fileName);

            if (!holidayFile.exists()) {
                // Create holidays directory if it doesn't exist
                File holidaysDir = new File(plugin.getDataFolder(), "holidays");
                if (!holidaysDir.exists()) {
                    holidaysDir.mkdirs();
                }
                plugin.saveResource(fileName, false);
            }

            YamlDocument config = YamlDocument.create(holidayFile);
            holidayConfigs.put(holidayName, config);
            return config;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load " + holidayName + ".yml: " + e.getMessage());
            return null;
        }
    }

    public void checkActiveHoliday() {
        checkActiveHoliday(false);
    }

    public void checkActiveHoliday(boolean forceRecheck) {
        // If a holiday is forced for testing, use that
        if (forcedHoliday != null) {
            currentActiveHoliday = forcedHoliday;
            plugin.getLogger().info("[Holiday] Using FORCED holiday: " + forcedHoliday);
            return;
        }

        LocalDate today = LocalDate.now();

        // Only check once per day for performance (unless forced)
        if (!forceRecheck && lastCheckedDate != null && lastCheckedDate.equals(today)) {
            return;
        }

        lastCheckedDate = today;
        currentActiveHoliday = null;

        // Always log the date being checked
        plugin.getLogger().info("[Holiday] Checking for active holidays on date: " + today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")));

        if (!holidaysConfig.getBoolean("Holidays.Enable", false)) {
            plugin.getLogger().warning("[Holiday] Holiday system is DISABLED in holidays.yml - Set 'Holidays.Enable: true' to enable");
            return;
        }

        List<String> holidays = holidaysConfig.getStringList("Holidays.List");
        plugin.getLogger().info("[Holiday] Checking " + holidays.size() + " configured holiday(s)...");

        for (String holidayName : holidays) {
            String startDateStr = holidaysConfig.getString("Holidays." + holidayName + ".Start-Date");
            String endDateStr = holidaysConfig.getString("Holidays." + holidayName + ".End-Date");

            if (startDateStr == null || endDateStr == null) {
                plugin.getLogger().warning("[Holiday] " + holidayName + " is missing Start-Date or End-Date in holidays.yml!");
                continue;
            }

            try {
                // Parse dates in MM/DD format
                LocalDate startDate = LocalDate.parse(today.getYear() + "/" + startDateStr,
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                LocalDate endDate = LocalDate.parse(today.getYear() + "/" + endDateStr,
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"));

                plugin.getLogger().info("[Holiday]   - " + holidayName + ": " + startDateStr + " to " + endDateStr + " (" + startDate + " to " + endDate + ")");

                // Handle year wrapping (e.g., Dec 25 to Jan 5)
                if (endDate.isBefore(startDate)) {
                    if (today.isBefore(startDate)) {
                        // We're in the next year part of the range
                        startDate = startDate.minusYears(1);
                    } else {
                        // We're in the same year part of the range
                        endDate = endDate.plusYears(1);
                    }
                    plugin.getLogger().info("[Holiday]     (Year-wrapping holiday, adjusted range: " + startDate + " to " + endDate + ")");
                }

                boolean inRange = (today.isEqual(startDate) || today.isAfter(startDate)) &&
                                 (today.isEqual(endDate) || today.isBefore(endDate));

                plugin.getLogger().info("[Holiday]     Match: " + (inRange ? "YES ✓" : "NO"));

                if (inRange) {
                    currentActiveHoliday = holidayName;
                    plugin.getLogger().info("[Holiday] ════════════════════════════════════════");
                    plugin.getLogger().info("[Holiday] ✓ ACTIVATED: " + holidayName);
                    plugin.getLogger().info("[Holiday] ✓ Date Range: " + startDateStr + " - " + endDateStr);
                    plugin.getLogger().info("[Holiday] ✓ 30 mob types will drop Halloween heads!");
                    plugin.getLogger().info("[Holiday] ════════════════════════════════════════");
                    break;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Holiday] ✗ Invalid date format for " + holidayName + ": " + e.getMessage());
                plugin.getLogger().warning("[Holiday]   Start: '" + startDateStr + "', End: '" + endDateStr + "'");
                plugin.getLogger().warning("[Holiday]   Expected format: MM/DD (e.g., 10/01 for October 1st)");
            }
        }

        // Log if no holiday is active
        if (currentActiveHoliday == null) {
            plugin.getLogger().info("[Holiday] No active holidays found for current date");
        }
    }

    public boolean isHolidayActive() {
        checkActiveHoliday();
        return currentActiveHoliday != null;
    }

    public String getActiveHoliday() {
        checkActiveHoliday();
        return currentActiveHoliday;
    }

    public EntityHead getHolidayHead(EntityType entityType, EntityHead defaultHead) {
        if (!isHolidayActive()) {
            return defaultHead;
        }

        YamlDocument holidayConfig = loadHolidayConfig(currentActiveHoliday);
        if (holidayConfig == null) {
            return defaultHead;
        }

        String entityName = entityType.name();
        String holidayHeadName = holidayConfig.getString("Mobs." + entityName);

        if (holidayHeadName == null) {
            // This mob doesn't have a holiday variant, return default
            return defaultHead;
        }

        try {
            EntityHead holidayHead = EntityHead.valueOf(holidayHeadName);

            if (plugin.getConfiguration().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Using holiday head " + holidayHeadName + " for " + entityName);
            }

            return holidayHead;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid holiday head name: " + holidayHeadName + " for " + entityName);
            return defaultHead;
        }
    }

    public void reload() {
        holidayConfigs.clear();
        currentActiveHoliday = null;
        lastCheckedDate = null;
        forcedHoliday = null;
        loadHolidaysConfig();
        checkActiveHoliday();
    }

    /**
     * Force a specific holiday to be active (for testing)
     */
    public void forceHoliday(String holidayName) {
        forcedHoliday = holidayName;
        currentActiveHoliday = holidayName;
        lastCheckedDate = null; // Clear cache
        plugin.getLogger().info("[Holiday] Forced holiday: " + holidayName);
    }

    /**
     * Clear forced holiday and return to normal date-based detection
     */
    public void clearForcedHoliday() {
        forcedHoliday = null;
        lastCheckedDate = null; // Force recheck
        checkActiveHoliday();
        plugin.getLogger().info("[Holiday] Cleared forced holiday, using date-based detection");
    }
}
