package com.indomitable.carrieraircraft.aircraft;

import com.google.gson.*;
import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 机型配置加载器。
 *
 * <p>从数据包 JSON 加载机型配置，路径格式：
 * {@code data/<namespace>/aircraft/<aircraft_id>.json}
 *
 * <p>内置机型作为默认值保证模组可玩，数据包可以覆盖或添加新机型。
 */
public class AircraftSpecLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "aircraft";

    private static AircraftSpecLoader instance;
    private final Map<String, AircraftSpec> specs = new HashMap<>();

    public AircraftSpecLoader() {
        super(GSON, DIRECTORY);
    }

    public static AircraftSpecLoader getInstance() {
        if (instance == null) {
            instance = new AircraftSpecLoader();
        }
        return instance;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        specs.clear();

        // 先加载内置默认值
        loadBuiltInSpecs();

        // 然后加载数据包配置（可以覆盖内置）
        int loaded = 0;
        int failed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                if (!json.isJsonObject()) {
                    IndomitableCarrierAircraft.LOGGER.error("Aircraft spec {} is not a JSON object", id);
                    failed++;
                    continue;
                }

                AircraftSpec spec = parseSpec(json.getAsJsonObject());
                String specId = id.getPath(); // 使用路径作为 ID（不含 namespace）
                specs.put(specId, spec);
                loaded++;

                IndomitableCarrierAircraft.LOGGER.info("Loaded aircraft spec: {}", specId);
            } catch (Exception e) {
                IndomitableCarrierAircraft.LOGGER.error("Failed to load aircraft spec {}: {}", id, e.getMessage(), e);
                failed++;
            }
        }

        IndomitableCarrierAircraft.LOGGER.info("Loaded {} aircraft specs ({} failed)", loaded, failed);
    }

    /**
     * 加载内置机型作为默认值。
     */
    private void loadBuiltInSpecs() {
        specs.put("b25", AircraftSpec.B25);
        specs.put("btd", AircraftSpec.BTD);
        specs.put("rocket_attacker", AircraftSpec.ROCKET_ATTACKER);
        specs.put("asw_patrol", AircraftSpec.ASW_PATROL);
        IndomitableCarrierAircraft.LOGGER.info("Loaded {} built-in aircraft specs", specs.size());
    }

    /**
     * 从 JSON 解析机型配置。
     */
    private AircraftSpec parseSpec(JsonObject json) throws JsonParseException {
        double speed = getDouble(json, "speed");
        double standbyHeight = getDouble(json, "standby_height");
        double attackHeight = getDouble(json, "attack_height");
        double attackRange = getDouble(json, "attack_range");
        double turnDistance = getDouble(json, "turn_distance");
        double health = getDouble(json, "health");
        int seaAmmoCapacity = getInt(json, "sea_ammo_capacity");
        int magazineCapacity = getInt(json, "magazine_capacity");
        int burstSize = getInt(json, "burst_size");
        float weaponDamage = getFloat(json, "weapon_damage");
        float explosionRadius = getFloat(json, "explosion_radius");
        double turretRange = getDouble(json, "turret_range", 0.0);
        double aswRange = getDouble(json, "asw_range", 0.0);

        AirWeaponMode airWeaponMode = parseEnum(json, "air_weapon_mode", AirWeaponMode.class, AirWeaponMode.NONE);
        Set<SeaAttackMode> seaAttackModes = parseEnumSet(json, "sea_attack_modes", SeaAttackMode.class);
        Set<AmmoType> allowedSeaAmmo = parseEnumSet(json, "allowed_sea_ammo", AmmoType.class);

        return new AircraftSpec(
                speed, standbyHeight, attackHeight, attackRange, turnDistance, health,
                seaAmmoCapacity, magazineCapacity, burstSize, weaponDamage, explosionRadius,
                turretRange, aswRange, airWeaponMode, seaAttackModes, allowedSeaAmmo
        );
    }

    // ==================== JSON 解析工具方法 ====================

    private double getDouble(JsonObject json, String key) throws JsonParseException {
        if (!json.has(key)) {
            throw new JsonParseException("Missing required field: " + key);
        }
        return json.get(key).getAsDouble();
    }

    private double getDouble(JsonObject json, String key, double defaultValue) {
        if (!json.has(key)) {
            return defaultValue;
        }
        return json.get(key).getAsDouble();
    }

    private int getInt(JsonObject json, String key) throws JsonParseException {
        if (!json.has(key)) {
            throw new JsonParseException("Missing required field: " + key);
        }
        return json.get(key).getAsInt();
    }

    private float getFloat(JsonObject json, String key) throws JsonParseException {
        if (!json.has(key)) {
            throw new JsonParseException("Missing required field: " + key);
        }
        return json.get(key).getAsFloat();
    }

    private <E extends Enum<E>> E parseEnum(JsonObject json, String key, Class<E> enumClass, E defaultValue) {
        if (!json.has(key)) {
            return defaultValue;
        }
        String value = json.get(key).getAsString().toUpperCase();
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            IndomitableCarrierAircraft.LOGGER.warn("Invalid enum value '{}' for {}, using default {}", value, key, defaultValue);
            return defaultValue;
        }
    }

    private <E extends Enum<E>> Set<E> parseEnumSet(JsonObject json, String key, Class<E> enumClass) throws JsonParseException {
        if (!json.has(key)) {
            throw new JsonParseException("Missing required field: " + key);
        }

        JsonArray array = json.getAsJsonArray(key);
        Set<E> result = EnumSet.noneOf(enumClass);

        for (JsonElement element : array) {
            String value = element.getAsString().toUpperCase();
            try {
                result.add(Enum.valueOf(enumClass, value));
            } catch (IllegalArgumentException e) {
                IndomitableCarrierAircraft.LOGGER.warn("Ignoring invalid enum value '{}' for {}", value, key);
            }
        }

        if (result.isEmpty()) {
            throw new JsonParseException("Field '" + key + "' must contain at least one valid value");
        }

        return result;
    }

    // ==================== 查询方法 ====================

    /**
     * 根据 ID 获取机型配置。
     *
     * @param id 机型 ID（如 "b25"、"btd"）
     * @return 机型配置，如果不存在则返回 B25 默认值
     */
    public AircraftSpec getSpec(String id) {
        return specs.getOrDefault(id, AircraftSpec.B25);
    }

    /**
     * 根据角色获取默认机型 ID。
     *
     * @param role 机型角色
     * @return 机型 ID
     */
    public static String getDefaultIdForRole(AircraftRole role) {
        return switch (role) {
            case BOMBER -> "b25";
            case DIVE_BOMBER, TORPEDO_BOMBER -> "btd";
            case ROCKET_ATTACKER -> "rocket_attacker";
            case ASW_PATROL -> "asw_patrol";
        };
    }

    /**
     * 检查机型是否存在。
     */
    public boolean hasSpec(String id) {
        return specs.containsKey(id);
    }

    /**
     * 获取所有已加载的机型 ID。
     */
    public Set<String> getAllSpecIds() {
        return specs.keySet();
    }
}
