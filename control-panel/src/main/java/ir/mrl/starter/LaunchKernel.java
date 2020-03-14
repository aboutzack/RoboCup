package ir.mrl.starter;

import kernel.*;
import kernel.ui.KernelGUI;
import kernel.ui.KernelStartupPanel;
import kernel.ui.ScoreGraph;
import kernel.ui.ScoreTable;
import org.dom4j.DocumentException;
import rescuecore2.Constants;
import rescuecore2.GUIComponent;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentInitialisationException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.connection.ConnectionException;
import rescuecore2.connection.ConnectionManager;
import rescuecore2.log.Logger;
import rescuecore2.misc.MutableBoolean;
import rescuecore2.misc.Pair;
import rescuecore2.misc.java.LoadableType;
import rescuecore2.misc.java.LoadableTypeProcessor;
import rescuecore2.registry.Registry;
import rescuecore2.scenario.Scenario;
import rescuecore2.score.ScoreFunction;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.WorldModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rescuecore2.misc.java.JavaTools.instantiate;

/**
 * @author Mostafa
 * @author Mahdi
 */
public class LaunchKernel {
    //private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(LaunchKernel.class);

    public static final String NO_STARTUP_MENU = "--nomenu";
    public static final String NO_GUI = "--nogui";
    public static final String AUTORUN = "--autorun";

    public static final String GIS_MANIFEST_KEY = "Gis";
    public static final String PERCEPTION_MANIFEST_KEY = "Perception";
    public static final String COMMUNICATION_MANIFEST_KEY = "CommunicationModel";

    public static final String COMMAND_COLLECTOR_KEY = "kernel.commandcollectors";
    public static final String TERMINATION_KEY = "kernel.termination";

    public static final String GIS_REGEX = "(.+WorldModelCreator).class";
    public static final String PERCEPTION_REGEX = "(.+Perception).class";
    public static final String COMMUNICATION_REGEX = "(.+CommunicationModel).class";

    public static final LoadableType GIS_LOADABLE_TYPE = new LoadableType(GIS_MANIFEST_KEY, GIS_REGEX, WorldModelCreator.class);
    public static final LoadableType PERCEPTION_LOADABLE_TYPE = new LoadableType(PERCEPTION_MANIFEST_KEY, PERCEPTION_REGEX, Perception.class);
    public static final LoadableType COMMUNICATION_LOADABLE_TYPE = new LoadableType(COMMUNICATION_MANIFEST_KEY, COMMUNICATION_REGEX, CommunicationModel.class);

    public static final String KERNEL_STARTUP_TIME_KEY = "kernel.startup.connect-time";
    public static final String COMMAND_FILTERS_KEY = "kernel.commandfilters";
    public static final String AGENT_REGISTRAR_KEY = "kernel.agents.registrar";
    public static final String GUI_COMPONENTS_KEY = "kernel.ui.components";

    public static KernelInfo createKernel(Config config, boolean showMenu) throws KernelException, DocumentException {
        KernelStartupOptions options = new KernelStartupOptions(config);
        // Show the chooser GUI
        if (showMenu) {
            final JDialog dialog = new JDialog((Frame) null, "Setup kernel options", true);
            KernelStartupPanel panel = new KernelStartupPanel(config, options);
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            JPanel buttons = new JPanel(new FlowLayout());
            buttons.add(okButton);
            buttons.add(cancelButton);
            dialog.getContentPane().add(panel, BorderLayout.CENTER);
            dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
            final MutableBoolean ok = new MutableBoolean(true);
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ok.set(true);
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ok.set(false);
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
            dialog.pack();
            dialog.setVisible(true);
            if (!ok.get()) {
                return null;
            }
        }
        WorldModelCreator gis = options.getWorldModelCreator();
        Perception perception = options.getPerception();
        CommunicationModel comms = options.getCommunicationModel();
        CommandFilter filter = makeCommandFilter(config);
        TerminationCondition termination = makeTerminationCondition(config);
        ScoreFunction score = makeScoreFunction(config);
        CommandCollector collector = makeCommandCollector(config);

        // Get the world model
        WorldModel<? extends Entity> worldModel = gis.buildWorldModel(config);
        Scenario scenario = gis.getScenario(config);
        // Create the kernel
        ScoreGraph graph = new ScoreGraph(score);
        Kernel kernel = new Kernel(config, perception, comms, worldModel, gis, filter, termination, graph, collector);
        // Create the component manager
        ComponentManager componentManager = new ComponentManager(kernel, worldModel, config, scenario);
        KernelInfo result = new KernelInfo(kernel, options, componentManager, makeGUIComponents(config, componentManager, perception, comms, termination, filter, graph, collector, score));
        return result;
    }

    public static void initialiseKernel(KernelInfo kernel, Config config, Registry registry) throws KernelException {
        registerInitialAgents(config, kernel.componentManager, kernel.kernel.getWorldModel());
        if (!config.getBooleanValue(KernelConstants.INLINE_ONLY_KEY, false)) {
            // Start the connection manager
            ConnectionManager connectionManager = new ConnectionManager();
            try {
                connectionManager.listen(config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY), registry, kernel.componentManager);
            } catch (IOException e) {
                throw new KernelException("Couldn't open kernel port", e);
            }
        }
    }

    public static void processJarFiles(Config config) throws IOException {
        LoadableTypeProcessor processor = new LoadableTypeProcessor(config);
        processor.setDirectory("jars");
        processor.addFactoryRegisterCallbacks(Registry.SYSTEM_REGISTRY);
        processor.addConfigUpdater(LoadableType.AGENT, config, KernelConstants.AGENTS_KEY);
        processor.addConfigUpdater(LoadableType.SIMULATOR, config, KernelConstants.SIMULATORS_KEY);
        processor.addConfigUpdater(LoadableType.VIEWER, config, KernelConstants.VIEWERS_KEY);
        processor.addConfigUpdater(LoadableType.COMPONENT, config, KernelConstants.COMPONENTS_KEY);
        processor.addConfigUpdater(GIS_LOADABLE_TYPE, config, KernelConstants.GIS_KEY);
        processor.addConfigUpdater(PERCEPTION_LOADABLE_TYPE, config, KernelConstants.PERCEPTION_KEY);
        processor.addConfigUpdater(COMMUNICATION_LOADABLE_TYPE, config, KernelConstants.COMMUNICATION_MODEL_KEY);
        Logger.info("Looking for gis, perception, communication, agent, simulator and viewer implementations");
        processor.process();
    }

    public static void waitForComponentManager(final KernelInfo kernel, Config config) throws KernelException {
        // Wait for all connections
        // Set up a CountDownLatch
        final CountDownLatch latch = new CountDownLatch(1);
        final long timeout = config.getIntValue(KERNEL_STARTUP_TIME_KEY, -1);
        Thread timeoutThread = null;
        if (timeout > 0) {
            timeoutThread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(timeout);
                        latch.countDown();
                    }
                    // CHECKSTYLE:OFF:EmptyBlock
                    catch (InterruptedException e) {
                        // Ignore
                    }
                    // CHECKSTYLE:ON:EmptyBlock
                }
            };
        }
        Thread waitThread = new Thread() {
            @Override
            public void run() {
                try {
                    kernel.componentManager.waitForAllAgents();
                    kernel.componentManager.waitForAllSimulators();
                    kernel.componentManager.waitForAllViewers();
                }
                // CHECKSTYLE:OFF:EmptyBlock
                catch (InterruptedException e) {
                    // Ignore
                }
                // CHECKSTYLE:ON:EmptyBlock
                latch.countDown();
            }
        };
        waitThread.start();
        if (timeoutThread != null) {
            timeoutThread.start();
        }
        // Wait at the latch until either everything is connected or the connection timeout expires
        Logger.info("Waiting for all agents, simulators and viewers to connect.");
        if (timeout > -1) {
            Logger.info("Connection timeout is " + timeout + "ms");
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            waitThread.interrupt();
            if (timeoutThread != null) {
                timeoutThread.interrupt();
            }
            throw new KernelException("Interrupted");
        }
    }

    public static void autostartComponents(KernelInfo info, Registry registry, KernelGUI gui, Config config) throws InterruptedException {
        KernelStartupOptions options = info.options;
        Collection<Callable<Void>> all = new ArrayList<Callable<Void>>();
        Config launchConfig = new Config(config);
        launchConfig.removeExcept(Constants.RANDOM_SEED_KEY, Constants.RANDOM_CLASS_KEY);
        for (Pair<String, Integer> next : options.getInlineComponents()) {
            if (next.second() > 0) {
                all.add(new ComponentStarter(next.first(), info.componentManager, next.second(), registry, gui, launchConfig));
            }
        }
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        service.invokeAll(all);
    }

    private static void registerInitialAgents(Config config, ComponentManager c, WorldModel<? extends Entity> model) throws KernelException {
        AgentRegistrar ar = instantiate(config.getValue(AGENT_REGISTRAR_KEY), AgentRegistrar.class);
        if (ar == null) {
            throw new KernelException("Couldn't instantiate agent registrar");
        }
        ar.registerAgents(model, config, c);
    }

    private static CommandFilter makeCommandFilter(Config config) {
        ChainedCommandFilter result = new ChainedCommandFilter();
        java.util.List<String> classNames = config.getArrayValue(COMMAND_FILTERS_KEY, null);
        for (String next : classNames) {
            Logger.debug("Command filter found: '" + next + "'");
            CommandFilter f = instantiate(next, CommandFilter.class);
            if (f != null) {
                result.addFilter(f);
            }
        }
        return result;
    }

    private static TerminationCondition makeTerminationCondition(Config config) {
        java.util.List<TerminationCondition> result = new ArrayList<TerminationCondition>();
        for (String next : config.getArrayValue(TERMINATION_KEY, null)) {
            TerminationCondition t = instantiate(next, TerminationCondition.class);
            if (t != null) {
                result.add(t);
            }
        }
        return new OrTerminationCondition(result);
    }

    private static ScoreFunction makeScoreFunction(Config config) {
        String className = config.getValue(Constants.SCORE_FUNCTION_KEY);
        ScoreFunction result = instantiate(className, ScoreFunction.class);
        return new ScoreTable(result);
    }

    private static CommandCollector makeCommandCollector(Config config) {
        java.util.List<String> classNames = config.getArrayValue(COMMAND_COLLECTOR_KEY);
        CompositeCommandCollector result = new CompositeCommandCollector();
        for (String next : classNames) {
            CommandCollector c = instantiate(next, CommandCollector.class);
            if (c != null) {
                result.addCommandCollector(c);
            }
        }
        return result;
    }

    private static java.util.List<GUIComponent> makeGUIComponents(Config config, Object... objectsToTest) {
        java.util.List<GUIComponent> result = new ArrayList<GUIComponent>();
        java.util.List<String> classNames = config.getArrayValue(GUI_COMPONENTS_KEY, null);
        for (String next : classNames) {
            Logger.debug("GUI component found: '" + next + "'");
            GUIComponent c = instantiate(next, GUIComponent.class);
            if (c != null) {
                result.add(c);
            }
        }
        for (Object next : objectsToTest) {
            if (next instanceof GUIComponent) {
                result.add((GUIComponent) next);
            }
        }
        return result;
    }

    private static class ComponentStarter implements Callable<Void> {
        private String className;
        private ComponentManager componentManager;
        private int count;
        private Registry registry;
        private KernelGUI gui;
        private Config config;

        public ComponentStarter(String className, ComponentManager componentManager, int count, Registry registry, KernelGUI gui, Config config) {
            this.className = className;
            this.componentManager = componentManager;
            this.count = count;
            this.registry = registry;
            this.gui = gui;
            this.config = config;
            Logger.debug("New ComponentStarter: " + className + " * " + count);
        }

        @Override
        public Void call() throws InterruptedException {
            Logger.debug("ComponentStarter running: " + className + " * " + count);
            ComponentLauncher launcher = new InlineComponentLauncher(componentManager, config);
            launcher.setDefaultRegistry(registry);
            Logger.info("Launching " + count + " instances of component '" + className + "'...");
            for (int i = 0; i < count; ++i) {
                rescuecore2.components.Component c = instantiate(className, rescuecore2.components.Component.class);
                if (c == null) {
                    break;
                }
                Logger.info("Launching " + className + " instance " + (i + 1) + "...");
                try {
                    c.initialise();
                    launcher.connect(c);
                    if (gui != null && c instanceof GUIComponent) {
                        gui.addGUIComponent((GUIComponent) c);
                    }
                    Logger.info(className + " instance " + (i + 1) + " launched successfully");
                } catch (ComponentConnectionException e) {
                    Logger.info(className + " instance " + (i + 1) + " failed: " + e.getMessage());
                    break;
                } catch (ComponentInitialisationException e) {
                    Logger.info(className + " instance " + (i + 1) + " failed", e);
                } catch (ConnectionException e) {
                    Logger.info(className + " instance " + (i + 1) + " failed", e);
                }
            }
            return null;
        }
    }

    public static class KernelInfo {
        Kernel kernel;
        KernelStartupOptions options;
        ComponentManager componentManager;
        java.util.List<GUIComponent> guiComponents;

        public KernelInfo(Kernel kernel, KernelStartupOptions options, ComponentManager componentManager, java.util.List<GUIComponent> otherComponents) {
            this.kernel = kernel;
            this.options = options;
            this.componentManager = componentManager;
            guiComponents = new ArrayList<GUIComponent>(otherComponents);
        }
    }

}
