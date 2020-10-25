package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.Velocity;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

@SuppressWarnings("nls")
public class TrainingServerMain {

	private static final String SHUTDOWN_PASS = "asdrsqer1223as";

    public TrainingServerMain() throws IOException {
    }

    private void sendFile(final String target, final HttpServletResponse response) throws IOException {
        this.setMimetype(target, response);
        final InputStream s = TrainingServerMain.class.getResourceAsStream(target);
        if (s == null) {
            this.sendError(404, "File not found " + target, response);
            return;
        }
        try {
            final OutputStream os = response.getOutputStream();
            try {
                this.copy(s, os);
            } finally {
                os.close();
            }
        } finally {
            s.close();
        }
    }

    private void setMimetype(final String file, final HttpServletResponse response) {
        if (file.contains(".html")) {
            response.setContentType("text/html;charset=UTF-8");
        } else if (file.contains(".css")) {
            response.setContentType("text/css;charset=UTF-8");
        } else if (file.contains(".js")) {
            response.setContentType("application/javascript;charset=UTF-8");
        } else if (file.contains(".png")) {
            response.setContentType("image/png");
        } else if (file.contains(".ico")) {
            response.setContentType("image/x-icon");
        }
    }

    private void copy(final InputStream s, final OutputStream os) throws IOException {
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = s.read(buffer)) >= 0) {
            os.write(buffer, 0, read);
        }
    }

    private void sendError(final int code, final String string, final HttpServletResponse response) throws IOException {
        response.sendError(code, string);
    }

    public static void main(final String[] args) throws Exception {
        if (TrainingServerMain.class.getResource("/index.html.static") == null) {
            System.out.println("something is wrong with the classpath");
            return;
        }
        TaskDB.init(new File(args[0]));
        Velocity.init();

        final TrainingServerMain m = new TrainingServerMain();
        Spark.before((final Request request, final Response response) -> DataLog.log(0, "calling URL " + request.url()));
        m.staticFile("/", "/index.html.static");
        m.staticFile("/index.html", "/index.html.static");
        m.staticFile("/experiment.js");
        m.staticFile("/experiment.css");
        m.staticFile("/codemirror.js");
        m.staticFile("/clike.js");
        m.staticFile("/codemirror.css");
        m.staticFile("/jquery.min.js");
        m.staticFile("/favicon.ico");
        m.staticFile("/set_logo.png");
        Spark.post("/login", m::login);
        Spark.post("/overview", m::overview);
        Spark.post("/nextTask", m::nextTask);
        Spark.post("/checkTask", m::checkTask);
        Spark.post("/solveTask", m::solveTask);
        Spark.post("/retryTask", m::retryTask);
        Spark.get("/shutdown/" + SHUTDOWN_PASS, m::shutdown);

        Runtime.getRuntime().addShutdownHook(new Thread() {
        	@Override
			public void run() {
        		cleanUp();
        	}
        });
    }

    private static void cleanUp() {
    	TaskDB.getInstance().shutdown();
    	StatisticsDB.getInstance().shutdown();
    }

    private Object login(final Request request, final Response response) {
        final String userName = request.queryParams("user");
        final Trainee u = UserDB.getUser(userName);
        u.setCurrentSessionStart(Instant.now());

        response.cookie("userName", userName);

    	return this.showOverview(u);
    }

    private Object overview(final Request request, final Response response) {
        return this.showOverview(this.getUserFromCookie(request));
    }

    private Object showOverview(final Trainee u) {
        final Map<String, Object> data = new HashMap<>();
        data.put("trainee", u);
        return this.velocity(data, "/start.html.vm");
    }

    private Object nextTask(final Request request, final Response response) {
        final Trainee u = this.getUserFromCookie(request);

        final Task task = TaskDB.getInstance().getNextTask(u);
        final Trial trial = u.startNewTrial(task);
    	return this.showTask(trial);
    }

    private Object solveTask(final Request request, final Response response) {
        final Trainee u = this.getUserFromCookie(request);

        final Task task = u.getCurrentTrial().getTask();
        final Map<String, Object> data = new HashMap<>();
        data.put("item", task);
        task.addContextData(data);
        data.put("solution", task.getSolution());
        return this.velocity(data, "/solution.html.vm");
    }

    private Object retryTask(final Request request, final Response response) {
        final Trainee u = this.getUserFromCookie(request);

        final Trial trial = u.retryCurrentTask();
        return this.showTask(trial);
    }

    private Object showTask(final Trial trial) {
        final Map<String, Object> data = new HashMap<>();
        final Task task = trial.getTask();
        data.put("item", task);
        data.put("trial", trial);
        task.addContextData(data);
        return this.velocity(data, task.getTemplate());
    }

    private Object checkTask(final Request request, final Response response) {
        final Trainee u = this.getUserFromCookie(request);

        final Trial trial = u.checkCurrentTrialAnswer(request);
        final FeedbackStatistics stats = this.determineStatistics(trial);
        StatisticsDB.getInstance().count(trial);

        final Map<String, Object> data = new HashMap<>();
        data.put("trial", trial);
        data.put("stats", stats);
        data.put("solution", trial.getTask().getSolution());
        data.put("trainee", u);
        return this.velocity(data, "/feedback.html.vm");
    }

    private FeedbackStatistics determineStatistics(final Trial trial) {
        return new FeedbackStatistics(StatisticsDB.getInstance().getFor(trial.getTask()), trial);
    }

    private Trainee getUserFromCookie(final Request request) {
        return UserDB.getUser(request.cookie("userName"));
    }

	private Object velocity(final Map<String, Object> data, final String template) {
    	return new VelocityTemplateEngine().render(new ModelAndView(data, template));
    }

    private void staticFile(final String path) {
    	this.staticFile(path, path);
	}

    private void staticFile(final String urlPath, final String cpPath) {
    	Spark.get(urlPath, (final Request request, final Response response) -> this.getStaticFile(request, response, cpPath));
    	Spark.head(urlPath, (final Request request, final Response response) -> this.getStaticFile(request, response, cpPath));
    }

	private Object getStaticFile(final Request request, final Response response, final String cpPath) throws IOException {
		this.sendFile(cpPath, response.raw());
		return null;
	}

    private Object shutdown(final Request request, final Response response) {
    	new Thread("stopper") {
    		@Override
			public void run() {
    	    	Spark.stop();
    	        Spark.awaitStop();
    	        try {
					DataLog.close();
				} catch (final IOException e) {
				}
    		}
    	} .start();
    	return "Stopping ...";
    }

}
