package io.github.asyncomatic.workers.basic;

import com.google.gson.Gson;
import io.github.asyncomatic.constants.Condition;
import io.github.asyncomatic.context.Context;
import io.github.asyncomatic.workers.Worker;
import io.github.asyncomatic.annotations.Retry;
import io.github.asyncomatic.annotations.Schedule;
import io.github.asyncomatic.annotations.ScheduleList;
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
        Method method = null;

        Context context = new Gson().fromJson(testJSON, Context.class);
        tests.add(context);

        while (tests.size() > 0) {
            context = tests.poll();

            logger.info("Calling test method: " + context.getClassName() + "#" + context.getMethodName());

            try {
                Class<?> testClazz = Class.forName(context.getClassName());

                Method[] declaredMethods = testClazz.getDeclaredMethods();
                for(Method declaredMethod : declaredMethods) {
                    if (declaredMethod.getName().equals(context.getMethodName())) {
                        method = declaredMethod;
                    }
                }

                if(method == null) {
                    throw new NoSuchMethodException("Missing method: "
                            + context.getClassName() + "#" + context.getMethodName());
                }

                Class[] parameterTypes = method.getParameterTypes();

                if(parameterTypes.length == 0) {
                    throw new NoSuchMethodException("Invalid signature for method: "
                            + context.getClassName() + "#" + context.getMethodName());
                }

                Class <?> stateClazz = parameterTypes[0];
                Object state;

                if(context.getTestData() == null || context.getTestData().length() == 0) {
                    state = stateClazz.getDeclaredConstructor().newInstance();
                } else {
                    state = new Gson().fromJson(context.getTestData(), stateClazz);
                }

                method.invoke(testClazz.getDeclaredConstructor().newInstance(), stateClazz.cast(state));
                context.setTestData(new Gson().toJson(stateClazz.cast(state)));

                logger.info("Execution of test method (STATUS: PASSED): "
                        + context.getClassName() + "#" + context.getMethodName());
                status = Condition.SUCCESS;

            } catch (InvocationTargetException e) {
                logger.info("Execution of test method (STATUS: FAILED): "
                        + context.getClassName() + "#" + context.getMethodName());
                status = Condition.FAILURE;
//                e.getCause().printStackTrace();

            } catch (ClassNotFoundException |  InstantiationException | NoSuchMethodException | IllegalAccessException e) {
                e.printStackTrace();
                continue;
            }

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
