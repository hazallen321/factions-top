package net.novucs.ftop.manager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.novucs.ftop.WorthType;
import net.novucs.ftop.entity.BlockPos;
import net.novucs.ftop.entity.ChunkPos;
import net.novucs.ftop.entity.ChunkWorth;
import net.novucs.ftop.entity.FactionWorth;
import net.novucs.ftop.util.GenericUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    public static DatabaseManager create(HikariConfig hikariConfig) throws SQLException {
        // Create the datasource.
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        // Test the connection.
        Connection connection = dataSource.getConnection();
        connection.close();

        // Return the new database manager.
        return new DatabaseManager(dataSource);
    }

    private DatabaseManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void init(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `world` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(40) NOT NULL UNIQUE," +
                "PRIMARY KEY (`id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `chunk` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`world_id` INT NOT NULL," +
                "`x` INT NOT NULL," +
                "`z` INT NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "FOREIGN KEY (`world_id`) REFERENCES world(`id`)," +
                "UNIQUE (`world_id`, `x`, `z`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `worth` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR (40) NOT NULL UNIQUE," +
                "PRIMARY KEY (`id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `chunk_worth` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`chunk_id` INT NOT NULL," +
                "`worth_id` INT NOT NULL," +
                "`worth` FLOAT NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "FOREIGN KEY (`chunk_id`) REFERENCES chunk(`id`)," +
                "FOREIGN KEY (`worth_id`) REFERENCES worth(`id`)," +
                "UNIQUE(`chunk_id`, `worth_id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `material` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(40) NOT NULL UNIQUE," +
                "PRIMARY KEY (`id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `chunk_material_count` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`chunk_id` INT NOT NULL," +
                "`material_id` INT NOT NULL," +
                "`count` INT NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "FOREIGN KEY (`chunk_id`) REFERENCES chunk(`id`)," +
                "FOREIGN KEY (`material_id`) REFERENCES material(`id`)," +
                "UNIQUE (`chunk_id`, `material_id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `spawner` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(40) NOT NULL UNIQUE," +
                "PRIMARY KEY (`id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `chunk_spawner_count` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`chunk_id` INT NOT NULL," +
                "`spawner_id` INT NOT NULL," +
                "`count` INT NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "FOREIGN KEY (`chunk_id`) REFERENCES chunk(`id`)," +
                "FOREIGN KEY (`spawner_id`) REFERENCES spawner(`id`)," +
                "UNIQUE (`chunk_id`, `spawner_id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `block` (" +
                "`id` INT NOT NULL AUTO_INCREMENT, " +
                "`world_id` INT NOT NULL, " +
                "`x` INT NOT NULL, " +
                "`y` INT NOT NULL, " +
                "`z` INT NOT NULL, " +
                "PRIMARY KEY (`id`), " +
                "FOREIGN KEY (`world_id`) REFERENCES world(`id`), " +
                "UNIQUE (`world_id`, `x`, `y`, `z`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `sign` (" +
                "`id` INT NOT NULL AUTO_INCREMENT, " +
                "`block_id` INT NOT NULL UNIQUE, " +
                "`rank` INT NOT NULL, " +
                "PRIMARY KEY (`id`), " +
                "FOREIGN KEY (`block_id`) REFERENCES block(`id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `faction` (" +
                "`id` VARCHAR(40) NOT NULL, " +
                "`name` VARCHAR(40) NOT NULL UNIQUE, " +
                "`total_worth` FLOAT NOT NULL, " +
                "`total_spawners` INT NOT NULL, " +
                "PRIMARY KEY (`id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `faction_worth` (" +
                "`id` INT NOT NULL AUTO_INCREMENT, " +
                "`faction_id` VARCHAR(40) NOT NULL, " +
                "`worth_id` INT NOT NULL, " +
                "`worth` FLOAT NOT NULL, " +
                "PRIMARY KEY (`id`), " +
                "FOREIGN KEY (`faction_id`) REFERENCES faction(`id`), " +
                "FOREIGN KEY (`worth_id`) REFERENCES worth(`id`), " +
                "UNIQUE (`faction_id`, `worth_id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `faction_material_count` (" +
                "`id` INT NOT NULL AUTO_INCREMENT, " +
                "`faction_id` VARCHAR(40) NOT NULL, " +
                "`material_id` INT NOT NULL, " +
                "`count` INT NOT NULL, " +
                "PRIMARY KEY (`id`), " +
                "FOREIGN KEY (`faction_id`) REFERENCES faction(`id`), " +
                "FOREIGN KEY (`material_id`) REFERENCES material(`id`), " +
                "UNIQUE (`faction_id`, `material_id`))");
        statement.executeUpdate();

        statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `faction_spawner_count` (" +
                "`id` INT NOT NULL AUTO_INCREMENT, " +
                "`faction_id` VARCHAR(40) NOT NULL, " +
                "`spawner_id` INT NOT NULL, " +
                "`count` INT NOT NULL, " +
                "PRIMARY KEY (`id`), " +
                "FOREIGN KEY (`faction_id`) REFERENCES faction(`id`), " +
                "FOREIGN KEY (`spawner_id`) REFERENCES spawner(`id`), " +
                "UNIQUE (`faction_id`, `spawner_id`))");
        statement.executeUpdate();
    }

    public Map<ChunkPos, ChunkWorth> load() throws SQLException {
        Map<ChunkPos, ChunkWorth> target = new HashMap<>();

        Map<WorthType, Double> worth;
        Map<Material, Integer> materialCount;
        Map<EntityType, Integer> spawnerCount;

        Connection connection = dataSource.getConnection();
        init(connection);

        Map<Integer, ChunkPos> chunks = getChunkMap(connection);

        for (Map.Entry<Integer, ChunkPos> entry : chunks.entrySet()) {
            worth = getChunkWorth(connection, entry.getKey());
            materialCount = getChunkMaterialCount(connection, entry.getKey());
            spawnerCount = getChunkSpawnerCount(connection, entry.getKey());
            target.put(entry.getValue(), new ChunkWorth(worth, materialCount, spawnerCount));
        }

        connection.close();
        return target;
    }

    public void close() {
        dataSource.close();
    }

    private Map<WorthType, Double> getChunkWorth(Connection connection, int chunkId) throws SQLException {
        Map<WorthType, Double> target = new EnumMap<>(WorthType.class);

        Map<Integer, WorthType> worthMap = getWorthMap(connection);

        PreparedStatement statement = connection.prepareStatement("SELECT `worth_id`,`worth` FROM `chunk_worth` WHERE `chunk_id`=?");
        statement.setInt(1, chunkId);
        ResultSet set = statement.executeQuery();

        while (set.next()) {
            WorthType worthType = worthMap.get(set.getInt("worth_id"));
            double worth = set.getDouble("worth");
            target.put(worthType, worth);
        }

        set.close();
        statement.close();

        return target;
    }

    private <T extends Enum<T>> Map<T, Integer> getCount(Connection connection, Class<T> clazz, String countType, int chunkId) throws SQLException {
        Map<T, Integer> target = new EnumMap<>(clazz);
        Map<Integer, T> supportMap = getEnumMap(connection, clazz, countType);

        PreparedStatement statement = connection.prepareStatement("SELECT `" + countType + "_id`,`count` " +
                "FROM `chunk_" + countType + "_count` WHERE `chunk_id`=?");
        statement.setInt(1, chunkId);
        ResultSet set = statement.executeQuery();

        while (set.next()) {
            T countTypeEnum = supportMap.get(set.getInt(countType + "_id"));
            int count = set.getInt("count");
            target.put(countTypeEnum, count);
        }

        set.close();
        statement.close();

        return target;
    }

    private Map<Material, Integer> getChunkMaterialCount(Connection connection, int chunkId) throws SQLException {
        return getCount(connection, Material.class, "material", chunkId);
    }

    private Map<EntityType, Integer> getChunkSpawnerCount(Connection connection, int chunkId) throws SQLException {
        return getCount(connection, EntityType.class, "spawner", chunkId);
    }

    private <T extends Enum<T>> Map<Integer, T> getEnumMap(Connection connection, Class<T> clazz, String table) throws SQLException {
        Map<Integer, T> target = new HashMap<>();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
        ResultSet set = statement.executeQuery();

        while (set.next()) {
            int id = set.getInt("id");
            Optional<T> parsed = GenericUtils.parseEnum(clazz, set.getString("name"));
            if (parsed.isPresent()) {
                target.put(id, parsed.get());
            }
        }

        set.close();
        statement.close();

        return target;
    }

    private Map<Integer, WorthType> getWorthMap(Connection connection) throws SQLException {
        return getEnumMap(connection, WorthType.class, "worth");
    }

    private Map<Integer, Material> getMaterialMap(Connection connection) throws SQLException {
        return getEnumMap(connection, Material.class, "material");
    }

    private Map<Integer, EntityType> getSpawnerMap(Connection connection) throws SQLException {
        return getEnumMap(connection, EntityType.class, "spawner");
    }

    private Map<Integer, ChunkPos> getChunkMap(Connection connection) throws SQLException {
        Map<Integer, ChunkPos> target = new HashMap<>();

        Map<Integer, String> worldMap = getWorldMap(connection);

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM `chunk`");
        ResultSet set = statement.executeQuery();

        while (set.next()) {
            int id = set.getInt("id");
            String world = worldMap.get(set.getInt("world_id"));
            int x = set.getInt("x");
            int z = set.getInt("z");
            target.put(id, ChunkPos.of(world, x, z));
        }

        set.close();
        statement.close();

        return target;
    }

    private Map<Integer, String> getWorldMap(Connection connection) throws SQLException {
        Map<Integer, String> target = new HashMap<>();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM `world`");
        ResultSet set = statement.executeQuery();

        while (set.next()) {
            int id = set.getInt("id");
            String name = set.getString("name");
            target.put(id, name);
        }

        set.close();
        statement.close();

        return target;
    }

    public void saveChunks(Collection<Map.Entry<ChunkPos, ChunkWorth>> chunkWorthEntries) throws SQLException {
        Connection connection = dataSource.getConnection();
        init(connection);

        for (Map.Entry<ChunkPos, ChunkWorth> entry : chunkWorthEntries) {
            int chunkId = saveChunk(connection, entry.getKey());
            saveChunkWorth(connection, chunkId, entry.getValue());
        }

        connection.close();
    }

    private void saveChunkWorth(Connection connection, int chunkId, ChunkWorth chunkWorth) throws SQLException {
        for (Map.Entry<WorthType, Double> entry : chunkWorth.getWorth().entrySet()) {
            saveChunkWorth(connection, chunkId, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Material, Integer> entry : chunkWorth.getMaterials().entrySet()) {
            saveChunkMaterial(connection, chunkId, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<EntityType, Integer> entry : chunkWorth.getSpawners().entrySet()) {
            saveChunkSpawner(connection, chunkId, entry.getKey(), entry.getValue());
        }
    }

    private int saveChunkMaterial(Connection connection, int chunkId, Material material, int count) throws SQLException {
        int materialId = saveMaterial(connection, material);
        int id = getChunkMaterialId(connection, chunkId, materialId);
        if (id > 0) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `chunk_material_count` SET `count` = ? WHERE `id` = ?");
            statement.setInt(1, count);
            statement.setInt(2, id);
            statement.executeUpdate();
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `chunk_material_count` (`chunk_id`, `material_id`, `count`) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, chunkId);
        statement.setInt(2, materialId);
        statement.setInt(3, count);

        statement.executeUpdate();
        ResultSet set = statement.getGeneratedKeys();

        set.next();
        return set.getInt(1);
    }

    private int getChunkMaterialId(Connection connection, int chunkId, int materialId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `chunk_material_count` WHERE `chunk_id` = ? AND `material_id` = ?");
        statement.setInt(1, chunkId);
        statement.setInt(2, materialId);
        ResultSet set = statement.executeQuery();
        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    private int saveChunkSpawner(Connection connection, int chunkId, EntityType spawner, int count) throws SQLException {
        int spawnerId = saveSpawner(connection, spawner);
        int id = getChunkSpawnerId(connection, chunkId, spawnerId);
        if (id > 0) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `chunk_spawner_count` SET `count` = ? WHERE `id` = ?");
            statement.setInt(1, count);
            statement.setInt(2, id);
            statement.executeUpdate();
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `chunk_spawner_count` (`chunk_id`, `spawner_id`, `count`) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, chunkId);
        statement.setInt(2, spawnerId);
        statement.setInt(3, count);

        statement.executeUpdate();
        ResultSet set = statement.getGeneratedKeys();

        set.next();
        return set.getInt(1);
    }

    private int getChunkSpawnerId(Connection connection, int chunkId, int spawnerId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `chunk_spawner_count` WHERE `chunk_id` = ? AND `spawner_id` = ?");
        statement.setInt(1, chunkId);
        statement.setInt(2, spawnerId);
        ResultSet set = statement.executeQuery();
        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    private int saveChunkWorth(Connection connection, int chunkId, WorthType worthType, double worth) throws SQLException {
        int worthId = saveWorth(connection, worthType);
        int id = getChunkWorthId(connection, chunkId, worthId);
        if (id > 0) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `chunk_worth` SET `worth` = ? WHERE `id` = ?");
            statement.setDouble(1, worth);
            statement.setInt(2, id);
            statement.executeUpdate();
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `chunk_worth` (`chunk_id`, `worth_id`, `worth`) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, chunkId);
        statement.setInt(2, worthId);
        statement.setDouble(3, worth);

        statement.executeUpdate();
        ResultSet set = statement.getGeneratedKeys();

        set.next();
        return set.getInt(1);
    }

    private int getChunkWorthId(Connection connection, int chunkId, int worthId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `chunk_worth` WHERE `chunk_id` = ? AND `worth_id` = ?");
        statement.setInt(1, chunkId);
        statement.setInt(2, worthId);
        ResultSet set = statement.executeQuery();
        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    private int saveChunk(Connection connection, ChunkPos pos) throws SQLException {
        int worldId = saveWorld(connection, pos.getWorld());
        int id = getChunkId(connection, worldId, pos.getX(), pos.getZ());
        if (id > 0) {
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `chunk` (`world_id`, `x`, `z`) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, worldId);
        statement.setInt(2, pos.getX());
        statement.setInt(3, pos.getZ());

        statement.executeUpdate();
        ResultSet set = statement.getGeneratedKeys();

        set.next();
        return set.getInt(1);
    }

    private int getChunkId(Connection connection, int worldId, int x, int z) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `chunk` WHERE `world_id` = ? AND `x` = ? AND `z` = ?");
        statement.setInt(1, worldId);
        statement.setInt(2, x);
        statement.setInt(3, z);
        ResultSet set = statement.executeQuery();
        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    private int saveName(Connection connection, String table, String name) throws SQLException {
        int id = getNameId(connection, table, name);
        if (id > 0) {
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + table + "` (`name`) VALUES(?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, name);
        statement.executeUpdate();

        ResultSet set = statement.getGeneratedKeys();
        set.next();
        return set.getInt(1);
    }

    private int getNameId(Connection connection, String table, String name) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `" + table + "` WHERE `name` = ?");
        statement.setString(1, name);
        ResultSet set = statement.executeQuery();
        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    private int saveMaterial(Connection connection, Material material) throws SQLException {
        return saveName(connection, "material", material.name());
    }

    private int saveSpawner(Connection connection, EntityType spawner) throws SQLException {
        return saveName(connection, "spawner", spawner.name());
    }

    private int saveWorld(Connection connection, String world) throws SQLException {
        return saveName(connection, "world", world);
    }

    private int saveWorth(Connection connection, WorthType worthType) throws SQLException {
        return saveName(connection, "worth", worthType.name());
    }

    public Multimap<Integer, BlockPos> loadSigns() throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT `block_id`, `rank` FROM `sign`");
        ResultSet set = statement.executeQuery();
        Multimap<Integer, BlockPos> signs = HashMultimap.create();

        while (set.next()) {
            int blockId = set.getInt("block_id");
            BlockPos pos = getBlock(connection, blockId);
            int rank = set.getInt("rank");
            signs.put(rank, pos);
        }

        connection.close();
        return signs;
    }

    private BlockPos getBlock(Connection connection, int blockId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `world_id`, `x`, `y`, `z` FROM `block` WHERE `id` = ?");
        statement.setInt(1, blockId);
        ResultSet set = statement.executeQuery();

        if (set.next()) {
            int worldId = set.getInt("world_id");
            String worldName = getWorldName(connection, worldId);
            if (worldName == null) return null;

            int x = set.getInt("x");
            int y = set.getInt("y");
            int z = set.getInt("z");
            return BlockPos.of(worldName, x, y, z);
        }

        return null;
    }

    private String getWorldName(Connection connection, int worldId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `name` FROM `world` WHERE `id` = ?");
        statement.setInt(1, worldId);
        ResultSet set = statement.executeQuery();
        return set.next() ? set.getString("name") : null;
    }

    public int saveSign(BlockPos pos, int rank) throws SQLException {
        Connection connection = dataSource.getConnection();
        int blockId = saveBlock(connection, pos);
        int id = getSign(connection, blockId);
        if (id > 0) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `sign` SET `rank` = ? WHERE `id` = ?");
            statement.setInt(1, rank);
            statement.setInt(2, id);
            statement.executeUpdate();
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `sign` (`block_id`, `rank`) VALUES(?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, blockId);
        statement.setInt(2, rank);

        statement.executeUpdate();
        ResultSet set = statement.getGeneratedKeys();

        set.next();
        id = set.getInt(1);
        connection.close();
        return id;
    }

    public void removeSign(BlockPos pos) throws SQLException {
        Connection connection = dataSource.getConnection();
        int worldId = getNameId(connection, "world", pos.getWorld());
        int blockId = getBlock(connection, worldId, pos.getX(), pos.getY(), pos.getZ());
        int signId = getSign(connection, blockId);
        if (signId < 0) return;

        PreparedStatement statement = connection.prepareStatement("DELETE FROM `sign` WHERE `id` = ?");
        statement.setInt(1, signId);
        statement.executeUpdate();
        connection.close();
    }

    private int getSign(Connection connection, int blockId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `sign` WHERE `block_id` = ?");
        statement.setInt(1, blockId);
        ResultSet set = statement.executeQuery();

        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    private int saveBlock(Connection connection, BlockPos pos) throws SQLException {
        int worldId = saveName(connection, "world", pos.getWorld());
        int id = getBlock(connection, worldId, pos.getX(), pos.getY(), pos.getZ());
        if (id > 0) {
            return id;
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO `block` (`world_id`, `x`, `y`, `z`) VALUES(?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, worldId);
        statement.setInt(2, pos.getX());
        statement.setInt(3, pos.getY());
        statement.setInt(4, pos.getZ());
        statement.executeUpdate();

        ResultSet set = statement.getGeneratedKeys();
        set.next();
        return set.getInt(1);
    }

    private int getBlock(Connection connection, int worldId, int x, int y, int z) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `block` " +
                "WHERE `world_id` = ? AND `x` = ? AND `y` = ? AND `z` = ?");
        statement.setInt(1, worldId);
        statement.setInt(2, x);
        statement.setInt(3, y);
        statement.setInt(4, z);
        ResultSet set = statement.executeQuery();

        if (set.next()) {
            return set.getInt("id");
        }

        return -1;
    }

    public void saveFactions(Collection<FactionWorth> factions) throws SQLException {
        Connection connection = dataSource.getConnection();
        init(connection);

        for (FactionWorth faction : factions) {
            saveFaction(connection, faction);
        }

        connection.close();
    }

    private boolean isFactionSaved(Connection connection, FactionWorth faction) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `faction` WHERE `id` = ?");
        statement.setString(1, faction.getFactionId());
        return statement.executeQuery().next();
    }

    private void saveFaction(Connection connection, FactionWorth faction) throws SQLException {
        if (isFactionSaved(connection, faction)) {
            updateFaction(connection, faction);
        } else {
            createFaction(connection, faction);
        }

        saveFactionWorth(connection, faction);
        saveFactionMaterials(connection, faction);
        saveFactionSpawners(connection, faction);
    }

    private void createFaction(Connection connection, FactionWorth faction) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO `faction` (`id`, `name`, `total_worth`, `total_spawners`) VALUES(?, ?, ?, ?)");
        statement.setString(1, faction.getFactionId());
        statement.setString(2, faction.getName());
        statement.setDouble(3, faction.getTotalWorth());
        statement.setInt(4, faction.getTotalSpawnerCount());
        statement.executeUpdate();
    }

    private void updateFaction(Connection connection, FactionWorth faction) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("UPDATE `faction` " +
                "SET `name` = ?, `total_worth` = ?, `total_spawners` = ? " +
                "WHERE `id` = ?");
        statement.setString(1, faction.getName());
        statement.setDouble(2, faction.getTotalWorth());
        statement.setInt(3, faction.getTotalSpawnerCount());
        statement.setString(4, faction.getFactionId());
        statement.executeUpdate();
    }

    private void saveFactionWorth(Connection connection, FactionWorth faction) throws SQLException {
        Map<Integer, WorthType> worthMap = getWorthMap(connection);
        PreparedStatement statement = connection.prepareStatement("SELECT `worth_id`, `worth` " +
                "FROM `faction_worth` " +
                "WHERE `faction_id` = ?");
        statement.setString(1, faction.getFactionId());
        ResultSet set = statement.executeQuery();

        Set<WorthType> worthTypes = Collections.newSetFromMap(new EnumMap<>(WorthType.class));

        while (set.next()) {
            int worthId = set.getInt("worth_id");
            WorthType type = worthMap.getOrDefault(worthId, null);
            if (type == null) {
                continue;
            }

            worthTypes.add(type);

            double worth = set.getDouble("worth");
            double localWorth = faction.getWorth(type);

            if (worth == localWorth) {
                continue;
            }

            statement = connection.prepareStatement("DELETE FROM `faction_worth`" +
                    "WHERE `faction_id` = ? AND `worth_id` = ?");
            statement.setString(1, faction.getFactionId());
            statement.setInt(2, worthId);
            statement.executeUpdate();
        }

        for (Map.Entry<WorthType, Double> entry : faction.getWorth().entrySet()) {
            int worthId = saveWorth(connection, entry.getKey());

            if (worthTypes.contains(entry.getKey())) {
                statement = connection.prepareStatement("UPDATE `faction_worth` " +
                        "SET `worth` = ? " +
                        "WHERE `faction_id` = ? AND `worth_id` = ?");
                statement.setDouble(1, entry.getValue());
                statement.setString(2, faction.getFactionId());
                statement.setInt(3, worthId);
                statement.executeUpdate();
            } else {
                statement = connection.prepareStatement("INSERT INTO `faction_worth` (`faction_id`, `worth_id`, `worth`) VALUES (?, ?, ?)");
                statement.setString(1, faction.getFactionId());
                statement.setInt(2, worthId);
                statement.setDouble(3, entry.getValue());
                statement.executeUpdate();
            }
        }
    }

    private void saveFactionMaterials(Connection connection, FactionWorth faction) throws SQLException {
        Map<Integer, Material> materialMap = getMaterialMap(connection);
        PreparedStatement statement = connection.prepareStatement("SELECT `material_id`, `count` " +
                "FROM `faction_material_count` " +
                "WHERE `faction_id` = ?");
        statement.setString(1, faction.getFactionId());
        ResultSet set = statement.executeQuery();

        Set<Material> materials = Collections.newSetFromMap(new EnumMap<>(Material.class));

        while (set.next()) {
            int materialId = set.getInt("material_id");
            Material material = materialMap.getOrDefault(materialId, null);
            if (material == null) {
                continue;
            }

            materials.add(material);

            int count = set.getInt("count");
            int localCount = faction.getMaterials().getOrDefault(material, 0);

            if (count == localCount) {
                continue;
            }

            statement = connection.prepareStatement("DELETE FROM `faction_material_count`" +
                    "WHERE `faction_id` = ? AND `material_id` = ?");
            statement.setString(1, faction.getFactionId());
            statement.setInt(2, materialId);
            statement.executeUpdate();
        }

        for (Map.Entry<Material, Integer> entry : faction.getMaterials().entrySet()) {
            int materialId = saveMaterial(connection, entry.getKey());

            if (materials.contains(entry.getKey())) {
                statement = connection.prepareStatement("UPDATE `faction_material_count` " +
                        "SET `count` = ? " +
                        "WHERE `faction_id` = ? AND `material_id` = ?");
                statement.setDouble(1, entry.getValue());
                statement.setString(2, faction.getFactionId());
                statement.setInt(3, materialId);
                statement.executeUpdate();
            } else {
                statement = connection.prepareStatement("INSERT INTO `faction_material_count` (`faction_id`, `material_id`, `count`) VALUES (?, ?, ?)");
                statement.setString(1, faction.getFactionId());
                statement.setInt(2, materialId);
                statement.setDouble(3, entry.getValue());
                statement.executeUpdate();
            }
        }
    }

    private void saveFactionSpawners(Connection connection, FactionWorth faction) throws SQLException {
        Map<Integer, EntityType> spawnerMap = getSpawnerMap(connection);
        PreparedStatement statement = connection.prepareStatement("SELECT `spawner_id`, `count` " +
                "FROM `faction_spawner_count` " +
                "WHERE `faction_id` = ?");
        statement.setString(1, faction.getFactionId());
        ResultSet set = statement.executeQuery();

        Set<EntityType> spawners = Collections.newSetFromMap(new EnumMap<>(EntityType.class));

        while (set.next()) {
            int spawnerId = set.getInt("spawner_id");
            EntityType spawner = spawnerMap.getOrDefault(spawnerId, null);
            if (spawner == null) {
                continue;
            }

            spawners.add(spawner);

            int count = set.getInt("count");
            int localCount = faction.getSpawners().getOrDefault(spawner, 0);

            if (count == localCount) {
                continue;
            }

            statement = connection.prepareStatement("DELETE FROM `faction_spawner_count`" +
                    "WHERE `faction_id` = ? AND `spawner_id` = ?");
            statement.setString(1, faction.getFactionId());
            statement.setInt(2, spawnerId);
            statement.executeUpdate();
        }

        for (Map.Entry<EntityType, Integer> entry : faction.getSpawners().entrySet()) {
            int spawnerId = saveSpawner(connection, entry.getKey());

            if (spawners.contains(entry.getKey())) {
                statement = connection.prepareStatement("UPDATE `faction_spawner_count` " +
                        "SET `count` = ? " +
                        "WHERE `faction_id` = ? AND `spawner_id` = ?");
                statement.setDouble(1, entry.getValue());
                statement.setString(2, faction.getFactionId());
                statement.setInt(3, spawnerId);
                statement.executeUpdate();
            } else {
                statement = connection.prepareStatement("INSERT INTO `faction_spawner_count` (`faction_id`, `spawner_id`, `count`) VALUES (?, ?, ?)");
                statement.setString(1, faction.getFactionId());
                statement.setInt(2, spawnerId);
                statement.setDouble(3, entry.getValue());
                statement.executeUpdate();
            }
        }
    }
}
