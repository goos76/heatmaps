package nl.will.heatmaps.servlets;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

@WebListener
public class StartUpListener implements ServletContextListener {
	private static Logger LOG;

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		LOG.info("heatmaps-web stopped");
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		PropertyConfigurator.configureAndWatch("log4j.properties");
		LOG = Logger.getLogger(StartUpListener.class);
		
		LOG.info("heatmaps-web started");

	}

}
