package io.github.asyncomatic.workers.basic;

import com.google.gson.Gson;
import io.github.asyncomatic.common.constants.Condition;
import io.github.asyncomatic.context.Context;
import io.github.asyncomatic.workers.Worker;
import io.github.asyncomatic.common.annotations.Retry;
import io.github.asyncomatic.common.annotations.Schedule;
import io.github.asyncomatic.common.annotations.ScheduleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;

public class BasicWorker implements Worker {
    private final LinkedList<Context> tests;
    private final HttpClient httpClient;
    private URI schedulerURI;

    static Logger logger = LoggerFactory.getLogger(BasicWorker.class);

    public BasicWorker(URI schedulerURI) {
        this.schedulerURI = schedulerURI;

        httpClient = HttpClient.newHttpClient();
        tests = new LinkedList<>();
    }

    public void execute(String testJSON) {
        int status;

        Context context = new Gson().fromJson(testJSON, Context.class);
        tests.add(context);

        while (tests.size() > 0) {
            Method method = null;
            context = tests.poll();

            logger.info("Calling test method: " + context.getClassName() + "#" + context.getMethodName());

            Class<?> testClazz;
            try {
                testClazz = Class.forName(context.getClassName());
            }catch(ClassNotFoundException e) {
                continue;
            }
            Method[] declaredMethods = testClazz.getDeclaredMethods();
            for(Method declaredMethod : declaredMethods) {
                if (declaredMethod.getName().equals(context.getMethodName())) {
                    method = declaredMethod;
                }
            }

            if(method == null) {
                logger.warn("Missing method: "
                        + context.getClassName() + "#" + context.getMethodName());
                continue;
            }

            Class[] parameterTypes = method.getParameterTypes();

            if(parameterTypes.length == 0) {
                logger.warn("Invalid signature for method: "
                        + context.getClassName() + "#" + context.getMethodName());
                continue;
            }

            Class <?> stateClazz = parameterTypes[0];
            Object state;

            try {
                if (context.getTestState() == null) {
                    state = stateClazz.getDeclaredConstructor().newInstance();
                } else {
                    state = new Gson().fromJson((String) context.getTestState(), stateClazz);
//                    state = context.getTestState();
                }
            }catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                logger.warn("Invalid state class specified: " + stateClazz.getName());
                continue;
            }

            try {
                method.invoke(testClazz.getDeclaredConstructor().newInstance(), stateClazz.cast(state));

                logger.info("Execution of test method (STATUS: PASSED): "
                        + context.getClassName() + "#" + context.getMethodName());
                status = Condition.SUCCESS;

            } catch (InvocationTargetException e) {
                logger.info("Execution of test method (STATUS: FAILED): "
                        + context.getClassName() + "#" + context.getMethodName());
                status = Condition.FAILURE;

            } catch(NoSuchMethodException |  IllegalAccessException |InstantiationException e) {
                logger.warn("Unknown error invoking method: "
                        + context.getClassName() + "#" + context.getMethodName());
                continue;
            }

            context.setTestState(state);
            processAnnotations(status, method, context);
        }
    }

    private void processAnnotations(int status, Method method, Context context) {
        if( ((status&(Condition.ERROR + Condition.FAILURE)) != 0) &&
                method.isAnnotationPresent(Retry.class) &&
                context.getRetryCount() < method.getAnnotation(Retry.class).count()) {

            Retry annotation = method.getAnnotation(Retry.class);

            Context newContext = new Gson().fromJson(new Gson().toJson(context), Context.class);
            newContext.setDelayDuration(annotation.delay() * annotation.units());
            newContext.setRetryCount(context.getRetryCount() + 1);

            schedule(newContext);

        }else {
            Schedule[] annotationList = new Schedule[0];

            if (method.isAnnotationPresent(ScheduleList.class)) {
                annotationList = method.getAnnotation(ScheduleList.class).value();

            } else if (method.isAnnotationPresent(Schedule.class)) {
                annotationList = method.getAnnotationsByType(Schedule.class);

            }

            for (Schedule annotation : annotationList) {
                if ((annotation.condition()&status) != 0) {
                    Context newContext = new Gson().fromJson(new Gson().toJson(context), Context.class);
                    newContext.setMethodName(annotation.method());
                    newContext.setDelayDuration(annotation.delay() * annotation.units());
                    newContext.setRetryCount(0);

                    schedule(newContext);
                }
            }
        }
    }

    private void schedule(Context context) {
        logger.info("Scheduling test method (DELAY: " + context.getDelayDuration() + "): "
                + context.getClassName() + "#" + context.getMethodName());

        if (context.getDelayDuration() > 0L) {
            try {
                HttpRequest req = HttpRequest.newBuilder(schedulerURI).
                        POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(context)))
                        .header("Content-type", "application/json").
                        build();

                httpClient.send(req, HttpResponse.BodyHandlers.discarding());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            tests.add(context);
        }
    }
}
