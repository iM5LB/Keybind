package com.keybind.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Scheduler shim that prefers the Paper/Folia regionized schedulers when they
 * exist on the server and falls back to the classic Bukkit scheduler on older
 * Paper builds. All lookups are reflective so the plugin still runs on
 * Paper 1.18.2 where these APIs are absent.
 */
public final class SchedulerCompat {

    private static final Method GET_GLOBAL_REGION_SCHEDULER = findBukkitMethod("getGlobalRegionScheduler");
    private static final Method GET_ASYNC_SCHEDULER = findBukkitMethod("getAsyncScheduler");
    private static final Method ENTITY_GET_SCHEDULER = findEntityMethod("getScheduler");

    private SchedulerCompat() {
    }

    private static Method findBukkitMethod(String name) {
        try {
            return org.bukkit.Bukkit.class.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findEntityMethod(String name) {
        try {
            return Entity.class.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method schedulerMethod(Object scheduler, String name, int paramCount) {
        if (scheduler == null) {
            return null;
        }
        for (Method method : scheduler.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        return null;
    }

    private static Object globalRegionScheduler() {
        if (GET_GLOBAL_REGION_SCHEDULER == null) {
            return null;
        }
        try {
            return GET_GLOBAL_REGION_SCHEDULER.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object asyncScheduler() {
        if (GET_ASYNC_SCHEDULER == null) {
            return null;
        }
        try {
            return GET_ASYNC_SCHEDULER.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object entityScheduler(Entity entity) {
        if (entity == null || ENTITY_GET_SCHEDULER == null) {
            return null;
        }
        try {
            return ENTITY_GET_SCHEDULER.invoke(entity);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Consumer<Object> consumer(Runnable task) {
        return ignored -> task.run();
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        Object scheduler = asyncScheduler();
        Method runNow = schedulerMethod(scheduler, "runNow", 2);
        if (runNow != null) {
            try {
                runNow.invoke(scheduler, plugin, consumer(task));
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void run(Entity entity, Plugin plugin, Runnable task) {
        Object scheduler = entityScheduler(entity);
        if (scheduler != null) {
            Method run = schedulerMethod(scheduler, "run", 3);
            if (run != null) {
                try {
                    run.invoke(scheduler, plugin, consumer(task), null);
                    return;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        Object global = globalRegionScheduler();
        if (global != null) {
            Method globalRun = schedulerMethod(global, "run", 2);
            if (globalRun != null) {
                try {
                    globalRun.invoke(global, plugin, consumer(task));
                    return;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runLater(Entity entity, Plugin plugin, long delayTicks, Runnable task) {
        Object scheduler = entityScheduler(entity);
        if (scheduler != null) {
            Method runDelayed = schedulerMethod(scheduler, "runDelayed", 4);
            if (runDelayed != null) {
                try {
                    runDelayed.invoke(scheduler, plugin, consumer(task), null, delayTicks);
                    return;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        Object global = globalRegionScheduler();
        if (global != null) {
            Method globalRunDelayed = schedulerMethod(global, "runDelayed", 3);
            if (globalRunDelayed != null) {
                try {
                    globalRunDelayed.invoke(global, plugin, consumer(task), delayTicks);
                    return;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
