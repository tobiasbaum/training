package de.set.assessmentUi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.Velocity;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class ServerMain {

    private final Map<Long, AssessmentSuite> assessments = new ConcurrentHashMap<>();

    public ServerMain() throws IOException {
    	final AssessmentSuite test = new AssessmentSuite(1234, "Herr Baum");
//    	test.addStep(new WorkingMemoryTest());
//    	test.addStep(new UnderstandingTask("/understanding/codeA"));
//    	test.addStep(new UnderstandingTask("/understanding/codeB"));
//    	test.addStep(new UnderstandingTask("/understanding/codeC"));
//    	test.addStep(new DefectFindTask("/defectFind/defA"));
//    	test.addStep(new DefectFindTask("/defectFind/defB"));
//    	test.addStep(new DefectFindTask("/defectFind/defC"));
    	test.addStep(new FinalQuestions());
    	this.assessments.put(test.getId(), test);
    }

    private void sendFile(final String target, final HttpServletResponse response) throws IOException {
        this.setMimetype(target, response);
        final InputStream s = ServerMain.class.getResourceAsStream(target);
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
        Velocity.init();

        final ServerMain m = new ServerMain();
        Spark.before((final Request request, final Response response) -> DataLog.log(0, "calling URL " + request.url()));
        m.staticFile("/", "/index.html");
        m.staticFile("/index.html");
        m.staticFile("/experiment.js");
        m.staticFile("/experiment.css");
        m.staticFile("/codemirror.js");
        m.staticFile("/codemirror.css");
        m.staticFile("/jquery.min.js");
        m.staticFile("/favicon.ico");
        m.staticFile("/set_logo.png");
        Spark.get("/assessment/*/start.html", m::assessmentStart);
        Spark.post("/assessment/*/step/*", m::assessmentStep);
        Spark.get("/shutdown/asdrsqer1223as", m::shutdown);
    }

    private Object assessmentStart(final Request request, final Response response) {
    	long id;
    	try {
    		id = Long.parseLong(request.splat()[0]);
    	} catch (final NumberFormatException e) {
    		return Spark.halt(404);
    	}
    	final AssessmentSuite a = this.assessments.get(id);
    	if (a == null) {
    		return Spark.halt(404);
    	}

    	final Map<String, Object> data = new HashMap<>();
    	data.put("assessment", a);
    	return this.velocity(data, "/start.html.vm");
    }

    private Object assessmentStep(final Request request, final Response response) {
    	long id;
    	try {
    		id = Long.parseLong(request.splat()[0]);
    	} catch (final NumberFormatException e) {
    		return Spark.halt(404);
    	}
    	final AssessmentSuite a = this.assessments.get(id);
    	if (a == null) {
    		return Spark.halt(404);
    	}

    	a.handleResultForCurrentStep(request);

    	int step;
    	try {
    		step = Integer.parseInt(request.splat()[1]);
    	} catch (final NumberFormatException e) {
    		return Spark.halt(404);
    	}

    	if (!a.isNextStep(step)) {
    		DataLog.log(a.getId(), "Invalid step " + step);
    		return "Ungültige Schrittnummer. Bitte nutzen Sie nur die Navigationsfunktionen der Webseite, nicht die des Browsers.";
    	}
    	a.setCurrentStep(step);

    	final AssessmentItem item = a.getStep(step);
    	final Map<String, Object> data = new HashMap<>();
    	data.put("assessment", a);
    	if (item != null) {
    		data.put("nextStep", step + 1);
    		data.put("item", item);
    		return this.velocity(data, item.getTemplate());
    	} else {
    		return this.velocity(data, "/closing.html.vm");
    	}
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
