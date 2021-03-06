package mem.kitek.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import mem.kitek.server.commons.Building;
import mem.kitek.server.commons.Hall;
import mem.kitek.server.commons.HallCategory;
import mem.kitek.server.sql.table.ColumnType;
import mem.kitek.server.sql.table.TableColumn;
import mem.kitek.server.sql.table.TableConstructor;
import mem.kitek.server.util.CachedInteger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by RINES on 20.10.17.
 */
public class HallManager {

    private final static Map<Integer, Integer> CAMS_TO_HALLS_RELATIVE = new HashMap<>();
    private final static Map<Integer, Set<Integer>> HALLS_TO_CAMS_RELATIVE = new HashMap<>();
    private final static Cache<Integer, Integer> CAMS_ONLINE = CacheBuilder.newBuilder()
            .expireAfterWrite(1L, TimeUnit.MINUTES).build();
    private final static LoadingCache<Integer, Integer> HALLS_ONLINE = CacheBuilder.newBuilder()
            .expireAfterWrite(1L, TimeUnit.MINUTES).build(new CacheLoader<Integer, Integer>() {
                @Override
                public Integer load(Integer hallId) {
                    return HALLS_TO_CAMS_RELATIVE.get(hallId).stream().mapToInt(cam -> {
                        Integer online = CAMS_ONLINE.getIfPresent(cam);
                        return online == null ? 0 : online;
                    }).sum();
                }
            });
    private final static CachedInteger SUMMARY_ONLINE = new CachedInteger(10000L, () -> HALLS_ONLINE.asMap().values().stream().mapToInt(i -> i).sum());
    private final static Map<Integer, Hall> HALLS = new HashMap<>();
    private final static Map<Integer, HallCategory> HALL_CATEGORIES = new HashMap<>();
    private final static Map<Integer, Building> BUILDINGS = new HashMap<>();

    public static void init() {
        new TableConstructor("cameras",
                new TableColumn("camera_id", ColumnType.INT).primary(true),
                new TableColumn("hall_id", ColumnType.INT)
        ).create(Bootstrap.getDatabase());
        new TableConstructor("hall_categories",
                new TableColumn("category_id", ColumnType.INT).primary(true),
                new TableColumn("category_name", ColumnType.VARCHAR_128)
        ).create(Bootstrap.getDatabase());
        new TableConstructor("halls",
                new TableColumn("hall_id", ColumnType.INT).primary(true),
                new TableColumn("hall_name", ColumnType.VARCHAR_128),
                new TableColumn("category_id", ColumnType.INT),
                new TableColumn("floor", ColumnType.INT)
        ).create(Bootstrap.getDatabase());
        try(ResultSet set = Bootstrap.getDatabase().query("SELECT * FROM cameras")) {
            while(set.next())
                registerCam(set.getInt(1), set.getInt(2));
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
        try(ResultSet set = Bootstrap.getDatabase().query("SELECT * FROM buildings")) {
            while(set.next()) {
                int id = set.getInt(1);
                Building building = new Building(id, set.getString(2));
                BUILDINGS.put(id, building);
            }
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
        try(ResultSet set = Bootstrap.getDatabase().query("SELECT * FROM hall_categories")) {
            while(set.next()) {
                int id = set.getInt(1);
                HallCategory category = new HallCategory(id, set.getString(2));
                HALL_CATEGORIES.put(id, category);
            }
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
        try(ResultSet set = Bootstrap.getDatabase().query("SELECT * FROM halls")) {
            while(set.next()) {
                int id = set.getInt(1);
                Hall hall = new Hall(id, set.getString(2), HALL_CATEGORIES.get(set.getInt(3)), BUILDINGS.get(set.getInt(4)), set.getInt(5));
                HALLS.put(id, hall);
            }
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void changeOnline(int camId, int online) {
        CAMS_ONLINE.put(camId, online);
        HALLS_ONLINE.refresh(CAMS_TO_HALLS_RELATIVE.get(camId));
    }

    public static int getHallOnline(int hallId) {
        if(!HALLS_TO_CAMS_RELATIVE.containsKey(hallId))
            return -1;
        return HALLS_ONLINE.getUnchecked(hallId);
    }

    public static int getFloorOnline(Building building, int floor) {
        return building.getPeopleOnFloor(floor);
    }

    public static int getSummaryOnline() {
        return SUMMARY_ONLINE.get();
    }

    public static Hall getHall(int id) {
        return HALLS.get(id);
    }

    public static HallCategory getHallCategory(int id) {
        return HALL_CATEGORIES.get(id);
    }

    public static Collection<Hall> getHallsByCategory(HallCategory category) {
        return HALLS.values().stream().filter(hall -> hall.getCategory() == category).collect(Collectors.toSet());
    }

    public static Building getBuilding(int id) {
        return BUILDINGS.get(id);
    }

    public static int getHallsSize() {
        return HALLS.size();
    }

    public static int getHallsCategoriesSize() {
        return HALL_CATEGORIES.size();
    }

    public static int getBuildingsSize() {
        return BUILDINGS.size();
    }

    public static int calculatePeopleOnFloor(Building building, int floor) {
        return HALLS.values().stream()
                .filter(hall -> hall.getBuilding() == building && hall.getFloor() == floor)
                .mapToInt(hall -> {
                    int people = getHallOnline(hall.getId());
                    return people == -1 ? 0 : people;
                })
                .sum();
    }

    private static void registerCam(int camId, int hallId) {
        CAMS_TO_HALLS_RELATIVE.put(camId, hallId);
        Set<Integer> backRelative = HALLS_TO_CAMS_RELATIVE.get(hallId);
        if(backRelative == null) {
            backRelative = new HashSet<>();
            HALLS_TO_CAMS_RELATIVE.put(hallId, backRelative);
        }
        backRelative.add(camId);
    }

    private static int getHallIdByCamId(int camId) {
        return CAMS_TO_HALLS_RELATIVE.get(camId);
    }

}
