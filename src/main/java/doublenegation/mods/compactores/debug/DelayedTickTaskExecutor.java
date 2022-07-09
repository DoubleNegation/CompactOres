package doublenegation.mods.compactores.debug;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiPredicate;

public class DelayedTickTaskExecutor {

    private static final List<Task> tasksForDelayedExecution = new ArrayList<>();

    public static void init() {
        Timer timer = new Timer("Compact Ores World Gen Debugging Delayed Execution Thread");
        timer.scheduleAtFixedRate(new TimerTask() {
            @SuppressWarnings("resource") // we do not want to shut down the server/client here
            @Override public void run() {
                synchronized(tasksForDelayedExecution) {
                    Iterator<Task> it = tasksForDelayedExecution.iterator();
                    while(it.hasNext()) {
                        Task task = it.next();
                        if(task.shouldExecuteYet().test(task.task(), task.eventLoop())) {
                            task.eventLoop().submit(task.task());
                            it.remove();
                        } else if((task.eventLoop() instanceof MinecraftServer) && ((MinecraftServer)task.eventLoop()).isStopped()) {
                            it.remove();
                        }
                    }
                }
            }
        }, 0, 100);
        MinecraftForge.EVENT_BUS.addListener((final ServerStoppedEvent e) -> {
            DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> timer::cancel);
        });
    }

    private static void submit(TickTask task, ReentrantBlockableEventLoop<? extends Runnable> eventLoop, BiPredicate<TickTask, ReentrantBlockableEventLoop<? extends Runnable>> pred) {
        synchronized(tasksForDelayedExecution) {
            tasksForDelayedExecution.add(new Task(task, eventLoop, pred));
        }
    }

    public static void submitDelayed(Runnable task, ReentrantBlockableEventLoop<? extends Runnable> eventLoop, int numTicksToDelay) {
        submit(new TickTask(eventLoop instanceof MinecraftServer ? ((MinecraftServer)eventLoop).getTickCount() : time(), task),
                eventLoop,
                eventLoop instanceof MinecraftServer ?
                        (tickTask, server) -> ((MinecraftServer)server).getTickCount() - numTicksToDelay >= tickTask.getTick() :
                        (tickTask, client) -> time() - /*50 MSPT*/50 * numTicksToDelay >= tickTask.getTick());
    }

    private static int time() {
        // this will break occasionally, but TickTask only stores int, and i need millisecond precision
        return (int) (System.currentTimeMillis() & 0x00000000FFFFFFFFL);
    }

    private record Task(TickTask task,
                        ReentrantBlockableEventLoop<? extends Runnable> eventLoop,
                        BiPredicate<TickTask, ReentrantBlockableEventLoop<? extends Runnable>> shouldExecuteYet) {}

}
