package nz.net.catalyst.icons;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

public class ProcExecTest {

    ExecutorServiceProcExecImpl executorService;
    AkkaFuturesProcExec akkaFutures;
    AkkaActorsProcExec akkaActors;
    List<ProcExec> procExecs;

    @Before
    public void setup() {
        executorService = new ExecutorServiceProcExecImpl();
        akkaActors = new AkkaActorsProcExec();
        akkaFutures = new AkkaFuturesProcExec();

        procExecs = new ArrayList<ProcExec>();
        procExecs.add(executorService);
        procExecs.add(akkaActors);
        procExecs.add(akkaFutures);
    }


    @Test
    public void executeProcess() {
        for (ProcExec procExec : procExecs) {
            String[] command = {"ls", "-l"};
            try {
                ExecResult result = procExec.execute(Arrays.asList(command));
                System.out.println(procExec.getClass().getCanonicalName() + " result = " + result);
                assertEquals(0, result.getExitStatus());
            } catch (Exception e) {
                e.printStackTrace();
                fail("no exception expected");
            }
        }
    }

    @Test
    public void executeExit1() {
        for (ProcExec procExec : procExecs) {
            String[] command = {"sh", "src/test/resources/exit1script.sh"};
            try {
                ExecResult result = procExec.execute(Arrays.asList(command));
                assertTrue(result.getExitStatus() == 1);
            } catch (Exception e) {
                e.printStackTrace();
                fail("no exception expected");
            }
        }
    }

    @Test
    public void timeout() {
        for (ProcExec procExec : procExecs) {
            String[] command = {"sleep", "21"};
            try {
                ExecResult result = procExec.execute(Arrays.asList(command));
            } catch (Exception e) {
                e.printStackTrace();
                assertEquals(TimeoutException.class, e.getClass());
            }
        }

    }

}
