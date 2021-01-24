package de.set.trainingUI;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.Velocity;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

@SuppressWarnings("nls")
public class TrainingServerMain {

	private static final String USER_SESSION_COOKIE = "userSession";
	private static final String USER_NAME_COOKIE = "userName";

	private static final String SHUTDOWN_PASS = "asdrsqer1223as";

	private final SecureRandom random = new SecureRandom();
	private final OAuth oAuth;

    public TrainingServerMain() throws IOException {
    	this.oAuth = new OAuth(
    			"e2a5802b4fb4398d7d51",
    			System.getProperty("oauth.client.secret"));
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
    	try {
	        if (TrainingServerMain.class.getResource("/index.html.static") == null) {
	            System.out.println("something is wrong with the classpath");
	            System.exit(-1);
	            return;
	        }
	        final List<File> taskDirectories = new ArrayList<>();
	        for (final String arg : args) {
	        	taskDirectories.add(new File(arg));
	        }
	        System.out.println(new Date() + " Task directories: " + taskDirectories);
	        TaskDB.init(taskDirectories);
	        System.out.println(new Date() + " Loaded " + TaskDB.getInstance().getTaskCount() + " tasks");
	        System.out.println(new Date() + " Initializing Velocity ...");
	        Velocity.init();

	        System.out.println(new Date() + " Starting server ...");
	        final TrainingServerMain m = new TrainingServerMain();
	        Spark.before((final Request request, final Response response) -> DataLog.log(request.cookie(USER_NAME_COOKIE), "call " + request.url()));
	        m.staticFile("/", "text/html", "/index.html.static");
	        m.staticFile("/index.html", "text/html", "/index.html.static");
	        m.staticFile("/experiment.js", "text/javascript");
	        m.staticFile("/experiment.css", "text/css");
	        m.staticFile("/codemirror.js", "text/javascript");
	        m.staticFile("/clike.js", "text/javascript");
	        m.staticFile("/codemirror.css", "text/css");
	        m.staticFile("/jquery.min.js", "text/javascript");
	        m.staticFile("/favicon.ico", "image/vnd.microsoft.icon");
	        m.staticFile("/set_logo.png", "image/png");
	        Spark.get("/login", m::login);
	        Spark.post("/login", m::login);
	        Spark.post("/overview", m::overview);
	        Spark.post("/nextTask", m::nextTask);
	        Spark.post("/checkTask", m::checkTask);
	        Spark.post("/solveTask", m::solveTask);
	        Spark.post("/retryTask", m::retryTask);
	        Spark.post("/registerProblemWithCurrentTask", m::registerProblemWithCurrentTask);
	        Spark.get("/shutdown/" + SHUTDOWN_PASS, m::shutdown);
	        Spark.get("/diagrams/tasksPerWeek.svg", m::diagramTasksPerWeek);
	        Spark.get("/diagrams/correctnessPerWeek.svg", m::diagramCorrectnessPerWeek);
	        Spark.get("/diagrams/durationPerWeek.svg", m::diagramDurationPerWeek);
	        Spark.get("/diagrams/trainingDurationPerWeek.svg", m::diagramTrainingDurationPerWeek);

	        Runtime.getRuntime().addShutdownHook(new Thread() {
	        	@Override
				public void run() {
	        		cleanUp();
	        	}
	        });
	        DataLog.log(null, "server started");
	        System.out.println(new Date() + " Server up and running");
    	} catch (final Throwable t) {
    		t.printStackTrace();
    		System.exit(-2);
    	}
    }

    private static void cleanUp() {
    	TaskDB.getInstance().shutdown();
    	StatisticsDB.getInstance().shutdown();
    }

    private Object login(final Request request, final Response response) throws IOException {
    	final String userName = this.oAuth.login(request);

        final Trainee u = UserDB.initUser(userName);

        response.cookie(USER_NAME_COOKIE, userName);
        final String userSession = Long.toString(this.random.nextLong());
        response.cookie(USER_SESSION_COOKIE, userSession);

        u.setCurrentSessionStart(Instant.now(), userSession);

        return this.handleMissingGoalOrShowOverview(request, u);
    }

    private Object overview(final Request request, final Response response) throws IOException {
        return this.handleMissingGoalOrShowOverview(request, this.getUserFromCookie(request));
    }

    private Object handleMissingGoalOrShowOverview(Request request, Trainee u) throws IOException {
    	if (request.queryParams("trainingGoal") != null) {
    		u.setTrainingGoal(Integer.parseInt(request.queryParams("trainingGoal")));
    	}

    	if (!u.hasTrainingGoal()) {
            final Map<String, Object> data = new HashMap<>();
            return this.velocity(data, "/chooseTrainingGoal.html.vm");
    	}

    	return this.showOverview(u);
    }

    private Object showOverview(final Trainee u) {
        final Map<String, Object> data = new HashMap<>();
        data.put("trainee", u);
        data.put("taskCount", u.getTrialCount());
        data.put("correctTaskCount", u.getCorrectTrialCount());
        data.put("correctStreak", u.getCurrentCorrectStreakLength());
        data.put("longestCorrectStreak", u.getLongestCorrectStreakLength());
        final int trainingDuration = lastEntry(DiagramData.getTrainingDurationPerWeek(u));
        data.put("currentWeek_missingForGoal", u.getTrainingGoal() - trainingDuration);
		data.put("currentWeek_trainingDuration", trainingDuration);
		data.put("currentWeek_correctShare", lastEntry(DiagramData.getCorrectnessPerWeek(u)));
		data.put("currentWeek_taskDuration", lastEntry(DiagramData.getDurationPerWeek(u)));
		data.put("currentWeek_taskCount", lastEntry(DiagramData.getTasksPerWeek(u)));
		data.put("traingGoalReachedCount", this.countGoalReached(DiagramData.getTrainingDurationPerWeek(u), u.getTrainingGoal()));
        return this.velocity(data, "/start.html.vm");
    }

    private int countGoalReached(CategoryDataset trainingDurationPerWeek, int trainingGoal) {
    	int count = 0;
    	for (int i = 0; i < trainingDurationPerWeek.getRowCount(); i++) {
    		final double value = trainingDurationPerWeek.getValue(i, 1).doubleValue();
    		if (value >= trainingGoal) {
    			count++;
    		}
    	}
		return count;
	}

	private static int lastEntry(CategoryDataset dataset) {
    	return dataset.getValue(dataset.getRowCount() - 1, 1).intValue();
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
        data.put("input", u.getCurrentSolution().formatInput());
        data.put("solution", u.getCurrentSolution().formatSolution());
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

        final String log = request.queryParams("logContent");
        for (final String logLine : log.split("\n")) {
            DataLog.log(u.getName(), "log from review;" + logLine);
        }

        final Trial trial = u.checkCurrentTrialAnswer(request);
        final FeedbackStatistics stats = this.determineStatistics(trial);
        StatisticsDB.getInstance().count(trial);

        final Map<String, Object> data = new HashMap<>();
        data.put("trial", trial);
        data.put("stats", stats);
        data.put("solution", u.getCurrentSolution().formatSolution());
        data.put("trainee", u);
        return this.velocity(data, "/feedback.html.vm");
    }

    private FeedbackStatistics determineStatistics(final Trial trial) {
        return new FeedbackStatistics(StatisticsDB.getInstance().getFor(trial.getTask()), trial);
    }

    private Trainee getUserFromCookie(final Request request) {
        final Trainee t = UserDB.getUser(request.cookie(USER_NAME_COOKIE));
        final String expectedSession = request.cookie(USER_SESSION_COOKIE);
        if (expectedSession == null || !expectedSession.equals(t.getSessionId())) {
        	throw new IllegalStateException("session ID is invalid");
        }
        return t;
    }

	private Object velocity(final Map<String, Object> data, final String template) {
    	return new VelocityTemplateEngine().render(new ModelAndView(data, template));
    }

    private void staticFile(final String path, String acceptType) {
    	this.staticFile(path, acceptType, path);
	}

    private void staticFile(final String urlPath, String acceptType, final String cpPath) {
    	Spark.get(urlPath, acceptType, (final Request request, final Response response) -> this.getStaticFile(request, response, cpPath));
    	Spark.head(urlPath, acceptType, (final Request request, final Response response) -> this.getStaticFile(request, response, cpPath));
    }

	private Object getStaticFile(final Request request, final Response response, final String cpPath) throws IOException {
		this.sendFile(cpPath, response.raw());
		return response;
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

    private Object registerProblemWithCurrentTask(final Request request, final Response response)
    	throws IOException {
        final Trainee u = this.getUserFromCookie(request);
		ProblemLog.getInstance().registerProblem(u, request.body());
        return "";
    }

    private Object diagramTasksPerWeek(final Request request, final Response response) {
    	return this.createDiagram(response, "bearbeitete Aufgaben", DiagramData.getTasksPerWeek(this.getUserFromCookie(request)));
    }

    private Object diagramCorrectnessPerWeek(final Request request, final Response response) {
    	return this.createDiagram(response, "Anteil korrekt", DiagramData.getCorrectnessPerWeek(this.getUserFromCookie(request)));
    }

    private Object diagramDurationPerWeek(final Request request, final Response response) {
    	return this.createDiagram(response, "Durchschnittszeit (s)", DiagramData.getDurationPerWeek(this.getUserFromCookie(request)));
    }

    private Object diagramTrainingDurationPerWeek(final Request request, final Response response) {
    	return this.createDiagram(response, "Trainingsdauer (Min.)", DiagramData.getTrainingDurationPerWeek(this.getUserFromCookie(request)));
    }

	private String createDiagram(Response response, String dataTitle, CategoryDataset dataset) {
    	final int widthOfSVG = 600;
    	final int heightOfSVG = 220;
    	final SVGGraphics2D svg2d = new SVGGraphics2D(widthOfSVG, heightOfSVG);

		final JFreeChart chart = ChartFactory.createLineChart(null, "KW", dataTitle, dataset);
		chart.removeLegend();
    	chart.draw(svg2d,new Rectangle2D.Double(0, 0, widthOfSVG, heightOfSVG));

    	response.type("image/svg+xml");
    	return svg2d.getSVGElement();
	}
}
