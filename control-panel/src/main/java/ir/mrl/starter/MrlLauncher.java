package ir.mrl.starter;

import com.mrl.debugger.LaunchMRLViewer;
import com.mrl.debugger.MrlAnimatedWorldModelViewer;
import com.mrl.debugger.MrlViewer;
import gis2.ScenarioException;
import gis2.scenario.CancelledByUserException;
import gis2.scenario.ScenarioEditor;
import kernel.*;
import kernel.ui.KernelGUI;
import maps.MapException;
import org.dom4j.DocumentException;
import rescuecore2.Constants;
import rescuecore2.GUIComponent;
import rescuecore2.LaunchComponents;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.*;
import rescuecore2.connection.ConnectionException;
import rescuecore2.log.LogException;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.registry.EntityFactory;
import rescuecore2.registry.MessageFactory;
import rescuecore2.registry.PropertyFactory;
import rescuecore2.registry.Registry;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.entities.StandardEntity;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.AlreadyBoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static rescuecore2.misc.java.JavaTools.instantiateFactory;

/**
 * @author Mostafa
 * @author Mahdi
 * @author Guanyu-cai
 */
public class MrlLauncher extends JFrame implements WindowListener {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlLauncher.class);

    private JCheckBox autoAgentCheckBox;
    private JPanel kernelPanel;
    private JTextField portTextField;
    private JTextField agentParamsTextField;
    private JTextField ATTextField;
    private JTextField ACTextField;
    private JTextField FBTextField;
    private JTextField FSTextField;
    private JTextField PFTextField;
    private JTextField POTextField;
    private JCheckBox preComputeCheckBox;
    private JCheckBox onThreadCheckBox;
    private JTextField hostTextField;
    private JTextField mapAddressTextField;
    private JButton mapSelectButton;
    private JCheckBox multipleRunCheckBox;
    private JTabbedPane mainTabbedPane;
    private JSpinner runNoSpinner;
    private JCheckBox showKernelCheckBox;
    private JCheckBox showViewerCheckBox;
    private JButton startAgentsButton;
    private JButton loadKernelButton;
    private JButton stepButton;
    private JButton runButton;
    private ScenarioEditor editor;
    private JPanel mapEditorPanel;
    private JPanel mapConfigEditorPanel;
    private JTextArea mapConfigTextArea;
    private JScrollPane configsPanel;
    private JPanel viewerPanel;

    private LaunchKernel.KernelInfo kernelInfo;
    //launch config params
    private File mrlKernelConfig = new File("mrlKernel.cfg");
    private String defMapAddressString = "map";
    private String defAutoRunAgentSelectedString = "autoAgent";
    private String defHostAddressString = "host";
    private String defPortAddressString = "port";
    private String defMultipleRunString = "multipleRun";
    private String defRunNoString = "runNo";
    private String defShowMrlViewerString = "showMrlViewer";
    private String defAgentOtherParamString = "agentOtherParam";
    private String defATParamString = "at";
    private String defACParamString = "ac";
    private String defFBParamString = "fb";
    private String defFSParamString = "fs";
    private String defPFParamString = "pf";
    private String defPOParamString = "po";
    private String defPreComputeString = "preCompute";
    private String defOnThreadString = "onThread";
    private String defShowKernelString = "showKernel";
    private String defMapAddress = "map address";
    private boolean defAutoRunAgentSelected = false;
    private String defHostAddress = "127.0.0.1";
    private String defPortAddress = "7000";
    private boolean defMultipleRun = false;
    private int defRunNo = 1;
    private boolean defShowMrlViewer = true;
    private boolean defPreCompute = true;
    private boolean defOnThread = false;
    private String defAgentOtherParam = "";
    private String defATParam = "-1";
    private String defACParam = "-1";
    private String defFBParam = "-1";
    private String defFSParam = "-1";
    private String defPFParam = "-1";
    private String defPOParam = "-1";
    private boolean defShowKernel = true;
    // end

    private boolean wait_for_agents = false;
    private boolean agents_connected = false;

    private File saveFile;

    public MrlLauncher() {
        initComponents();
    }

    protected void launchAgents(String[] agentArgs, boolean precompute) throws IOException {

        File precomputeFile = new File("precompute.sh");
        File startFile = new File("start.sh");
        if (precompute && !precomputeFile.exists()) {
            System.err.println("Error: Unable to find precompute.sh files.");
            precompute = false;
        }

        if (!precompute && !startFile.exists()) {
            System.err.println("Error: Unable to find start.sh or precompute.sh files.");
            return;
        }

//        Process proc = Runtime.getRuntime().exec("java -jar C:\\A.jar");
        String script;
        String scriptFile = precompute ? "precompute.sh" : "start.sh";
        if (isWindows()) {
            script = "cmd /c start " + scriptFile;
        } else {
            script = "./" + scriptFile;
        }
        for (String arg : agentArgs) {
            script += " " + arg;
        }
        System.out.println("############Script to run: " + script);
        Process proc = Runtime.getRuntime().exec(script);


//        ProcessBuilder pb = new ProcessBuilder("myshellScript.sh");
//        Map<String, String> env = pb.environment();
//        env.put("VAR1", "myValue");
//        env.remove("OTHERVAR");
//        env.put("VAR2", env.get("VAR1") + "suffix");
//        pb.directory(new File("myDir"));
//        Process p = pb.start();
    }

    private void initComponents() {

        try {

            FileReader fr = new FileReader(mrlKernelConfig);
            BufferedReader br = new BufferedReader(fr);
            String p = br.readLine();
            while (p != null) {
                String[] param = p.split(":", 2);
                if (param[0].equalsIgnoreCase(defMapAddressString)) {
                    defMapAddress = param[1];
                } else if (param[0].equalsIgnoreCase(defAutoRunAgentSelectedString)) {
                    defAutoRunAgentSelected = Boolean.valueOf(param[1]);
                } else if (param[0].equalsIgnoreCase(defHostAddressString)) {
                    defHostAddress = param[1];
                } else if (param[0].equalsIgnoreCase(defPortAddressString)) {
                    defPortAddress = param[1];
                } else if (param[0].equalsIgnoreCase(defMultipleRunString)) {
                    defMultipleRun = Boolean.valueOf(param[1]);
                } else if (param[0].equalsIgnoreCase(defRunNoString)) {
                    defRunNo = Integer.valueOf(param[1]);
                } else if (param[0].equalsIgnoreCase(defShowMrlViewerString)) {
                    defShowMrlViewer = Boolean.valueOf(param[1]);
                } else if (param[0].equalsIgnoreCase(defAgentOtherParamString)) {
                    defAgentOtherParam = param[1];
                } else if (param[0].equalsIgnoreCase(defATParamString)) {
                    defATParam = param[1];
                } else if (param[0].equalsIgnoreCase(defACParamString)) {
                    defACParam = param[1];
                } else if (param[0].equalsIgnoreCase(defFBParamString)) {
                    defFBParam = param[1];
                } else if (param[0].equalsIgnoreCase(defFSParamString)) {
                    defFSParam = param[1];
                } else if (param[0].equalsIgnoreCase(defPFParamString)) {
                    defPFParam = param[1];
                } else if (param[0].equalsIgnoreCase(defPOParamString)) {
                    defPOParam = param[1];
                } else if (param[0].equalsIgnoreCase(defPreComputeString)) {
                    defPreCompute = Boolean.valueOf(param[1]);
                } else if (param[0].equalsIgnoreCase(defOnThreadString)) {
                    defOnThread = Boolean.valueOf(param[1]);
                } else if (param[0].equalsIgnoreCase(defShowKernelString)) {
                    defShowKernel = Boolean.valueOf(param[1]);
                } else {
                    Logger.error("data error in mrlKernel.config");
                }
                p = br.readLine();
            }
            br.close();
            fr.close();
        } catch (Exception ignore) {
        }

//        progLabel = new JLabel();
        mainTabbedPane = new JTabbedPane();
        JPanel mainPanel = new JPanel();
        JLayeredPane mainLayeredPane = new JLayeredPane();
        JLabel mapAddressLabel = new JLabel();
        mapAddressTextField = new JTextField();
        mapSelectButton = new JButton();
        JPanel propsPanel = new JPanel();
        autoAgentCheckBox = new JCheckBox();
        multipleRunCheckBox = new JCheckBox();
        showKernelCheckBox = new JCheckBox();
        JLabel runNLabel = new JLabel();
        runNoSpinner = new JSpinner();
        showViewerCheckBox = new JCheckBox();
        JLabel portLabel = new JLabel();
        portTextField = new JTextField();
        JLabel hostLabel = new JLabel();
        hostTextField = new JTextField();
        JLabel agentParamsLabel = new JLabel();
        agentParamsTextField = new JTextField();
        JLabel ATLabel = new JLabel();
        ATTextField = new JTextField();
        JLabel ACLabel = new JLabel();
        ACTextField = new JTextField();
        JLabel FBLabel = new JLabel();
        FBTextField = new JTextField();
        JLabel FSLabel = new JLabel();
        FSTextField = new JTextField();
        JLabel PFLabel = new JLabel();
        PFTextField = new JTextField();
        JLabel POLabel = new JLabel();
        POTextField = new JTextField();
        preComputeCheckBox = new JCheckBox();
        onThreadCheckBox = new JCheckBox();
        mapEditorPanel = new JPanel();
        mapConfigEditorPanel = new JPanel();
        configsPanel = new JScrollPane();
        kernelPanel = new JPanel();
        viewerPanel = new JPanel();
        JPanel kernelBPanel = new JPanel();
        stepButton = new JButton();
        runButton = new JButton();
        startAgentsButton = new JButton();
        loadKernelButton = new JButton();
        JButton reRunButton = new JButton();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(930, 520));

//        progLabel.setFont(new Font("Vani", Font.BOLD, 12)); // NOI18N
//        progLabel.setHorizontalAlignment(SwingConstants.CENTER);
//        progLabel.setText("MRL - RSL kernel and code launcher (2013)");

        mainPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        mainPanel.setEnabled(false);
        mainPanel.setMaximumSize(new Dimension(540, 200));
        mainPanel.setMinimumSize(new Dimension(540, 200));
        mainPanel.setPreferredSize(new Dimension(540, 200));

        mapAddressLabel.setLabelFor(mapAddressTextField);
        mapAddressLabel.setText("Map Address:");
        mapAddressLabel.setFocusable(false);
        mapAddressLabel.setBounds(10, 10, 66, 14);
        mainLayeredPane.add(mapAddressLabel, JLayeredPane.DEFAULT_LAYER);

        mapAddressTextField.setText(defMapAddress);
        mapAddressTextField.setEnabled(false);
        mapAddressTextField.setBounds(80, 10, 359, 20);
        mainLayeredPane.add(mapAddressTextField, JLayeredPane.DEFAULT_LAYER);

        mapSelectButton.setText("select map");
        mapSelectButton.addActionListener(evt -> mapSelectButtonActionPerformed());
        mapSelectButton.setBounds(450, 10, 83, 23);
        mainLayeredPane.add(mapSelectButton, JLayeredPane.DEFAULT_LAYER);

        propsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "parameters"));

        autoAgentCheckBox.setText("Autorun agents");
        autoAgentCheckBox.setSelected(defAutoRunAgentSelected);

        multipleRunCheckBox.setText("Multiple run");
        multipleRunCheckBox.setEnabled(false);
        multipleRunCheckBox.setSelected(defMultipleRun);

        showKernelCheckBox.setText("Show kernel");
        showKernelCheckBox.setEnabled(false);
        showKernelCheckBox.setSelected(defShowKernel);

        runNLabel.setText("run number:");
        runNLabel.setEnabled(false);

        runNoSpinner.setName("runNo"); // NOI18N
        runNoSpinner.setEnabled(false);
        runNoSpinner.setValue(defRunNo);

        showViewerCheckBox.setText("Load Mrl visual debugger");
        showViewerCheckBox.setSelected(defShowMrlViewer);

        portLabel.setText("port:");

        portTextField.setText(defPortAddress);

        hostLabel.setText("Host:");

        hostTextField.setText(defHostAddress);

        agentParamsLabel.setText("Agent params:");

        agentParamsTextField.setText(defAgentOtherParam);

        ATLabel.setText("AT:");
        ATTextField.setText(defATParam);
        ACLabel.setText("AC:");
        ACTextField.setText(defACParam);
        FBLabel.setText("FB:");
        FBTextField.setText(defFBParam);
        FSLabel.setText("FS:");
        FSTextField.setText(defFSParam);
        PFLabel.setText("PF:");
        PFTextField.setText(defPFParam);
        POLabel.setText("PO:");
        POTextField.setText(defPOParam);

        preComputeCheckBox.setText("preCompute");
        preComputeCheckBox.setSelected(defPreCompute);
        onThreadCheckBox.setText("Start on thread");
        onThreadCheckBox.setSelected(defOnThread);

        GroupLayout propsPanelLayout = new GroupLayout(propsPanel);
        propsPanel.setLayout(propsPanelLayout);
        propsPanelLayout.setHorizontalGroup(
                propsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(propsPanelLayout.createSequentialGroup()
                                .addGroup(propsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(propsPanelLayout.createSequentialGroup()
                                                .addGap(6, 6, 6)
                                                .addComponent(agentParamsLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(agentParamsTextField)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(showKernelCheckBox)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(propsPanelLayout.createSequentialGroup()
                                                .addComponent(preComputeCheckBox)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
//                                                .addComponent(onThreadCheckBox)
//                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(showViewerCheckBox)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
                                        .addGroup(propsPanelLayout.createSequentialGroup()
                                                .addGap(6, 6, 6)
                                                .addComponent(ATLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(ATTextField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(ACLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(ACTextField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(FBLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(FBTextField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(FSLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(FSTextField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(PFLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(PFTextField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(POLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(POTextField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
                                        .addGroup(propsPanelLayout.createSequentialGroup()
                                                .addComponent(autoAgentCheckBox)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(hostLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(hostTextField, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
                                                .addGap(10, 10, 10)
                                                .addComponent(portLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(portTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(multipleRunCheckBox)
                                                .addGap(10, 10, 10)
                                                .addComponent(runNLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(runNoSpinner, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        propsPanelLayout.setVerticalGroup(
                propsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(propsPanelLayout.createSequentialGroup()
                                .addGroup(propsPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(autoAgentCheckBox)
                                        .addComponent(runNLabel)
                                        .addComponent(runNoSpinner, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(portLabel)
                                        .addComponent(portTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hostLabel)
                                        .addComponent(hostTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(multipleRunCheckBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(propsPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(ATLabel)
                                        .addComponent(ATTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ACLabel)
                                        .addComponent(ACTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FBLabel)
                                        .addComponent(FBTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FSLabel)
                                        .addComponent(FSTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PFLabel)
                                        .addComponent(PFTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(POLabel)
                                        .addComponent(POTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(propsPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(preComputeCheckBox)
//                                        .addComponent(onThreadCheckBox)
                                        .addComponent(showViewerCheckBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(propsPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(agentParamsLabel)
                                        .addComponent(agentParamsTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(showKernelCheckBox))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        propsPanel.setBounds(10, 40, 530, 150);
        mainLayeredPane.add(propsPanel, JLayeredPane.DEFAULT_LAYER);

        GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
                mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(mainLayeredPane, GroupLayout.PREFERRED_SIZE, 545, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(116, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
                mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(mainLayeredPane, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(77, Short.MAX_VALUE))
        );

        mainTabbedPane.addTab("properties", mainPanel);

        importMapEditor();
        mainTabbedPane.addTab("Map Editor", mapEditorPanel);

        importMapConfigEditor();
        mainTabbedPane.addTab("Map Config Editor", mapConfigEditorPanel);

        GroupLayout kernelPanelLayout = new GroupLayout(kernelPanel);
        kernelPanel.setLayout(kernelPanelLayout);
        kernelPanelLayout.setHorizontalGroup(
                kernelPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 675, Short.MAX_VALUE)
        );
        kernelPanelLayout.setVerticalGroup(
                kernelPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 282, Short.MAX_VALUE)
        );

        mainTabbedPane.addTab("Kernel", kernelPanel);

        GroupLayout viewerPanelLayout = new GroupLayout(viewerPanel);
        viewerPanel.setLayout(viewerPanelLayout);
        viewerPanelLayout.setHorizontalGroup(
                viewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 675, Short.MAX_VALUE)
        );
        viewerPanelLayout.setVerticalGroup(
                viewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 282, Short.MAX_VALUE)
        );

        mainTabbedPane.addTab("Viewer", viewerPanel);

        startAgentsButton.setText("Start agents");
        startAgentsButton.setEnabled(false);
        startAgentsButton.setMargin(new Insets(2, 4, 2, 4));
        startAgentsButton.addActionListener(evt -> {
            startAgentsButton.setEnabled(false);
            stepButton.setEnabled(false);
            runButton.setEnabled(false);
            autoAgentCheckBox.setEnabled(false);
            hostTextField.setEnabled(false);
            portTextField.setEnabled(false);
            showViewerCheckBox.setEnabled(false);
            ATTextField.setEnabled(false);
            ACTextField.setEnabled(false);
            FBTextField.setEnabled(false);
            FSTextField.setEnabled(false);
            PFTextField.setEnabled(false);
            POTextField.setEnabled(false);
            preComputeCheckBox.setEnabled(false);
            onThreadCheckBox.setEnabled(false);
            agentParamsTextField.setEnabled(false);
            new Thread(new StartAgentsThread()).start();
        });
        reRunButton.setText("Re-Run");
        reRunButton.setEnabled(false);
        reRunButton.setMargin(new Insets(2, 4, 2, 4));
        reRunButton.addActionListener(evt -> {
//                reRunButton.setEnabled(false);
//                mapSelectButton.setEnabled(false);
//                if (!loadKernelButton.isEnabled()) {
//                    initComponents();
//                }
            if (kernelInfo != null && kernelInfo.kernel != null && !kernelInfo.kernel.hasTerminated()) {
                //kernelInfo.kernel.shutdown();
                new Thread(new LoadKernelThread()).start();
                try {
                    Thread.currentThread().sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new Thread(new LoadMiscSimulatorThread()).start();
                new Thread(new LoadClearSimulatorThread()).start();
                new Thread(new LoadCollapseSimulatorThread()).start();
                new Thread(new LoadFireSimulatorThread()).start();
                new Thread(new LoadIgnitionSimulatorThread()).start();
                new Thread(new LoadTrafficSimulatorThread()).start();

//                    loadKernelButton.setEnabled(true);
            }
//                new Thread(new LoadKernelThread()).start();
//
//                if (autoAgentCheckBox.isSelected()) {
//                    new Thread(new StartAgentsThread()).start();
//                }
        });

        loadKernelButton.setText("Load kernel");
        loadKernelButton.setMargin(new Insets(2, 4, 2, 4));
        loadKernelButton.addActionListener(evt -> {
            if (getMapAddress(false) != null) {
                loadKernelButton.setEnabled(false);
                mapSelectButton.setEnabled(false);
                new Thread(new LoadKernelThread()).start();
                try {
                    Thread.currentThread().sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new Thread(new LoadMiscSimulatorThread()).start();
                new Thread(new LoadClearSimulatorThread()).start();
                new Thread(new LoadCollapseSimulatorThread()).start();
                new Thread(new LoadFireSimulatorThread()).start();
                new Thread(new LoadIgnitionSimulatorThread()).start();
                new Thread(new LoadTrafficSimulatorThread()).start();
            } else {
                JOptionPane.showMessageDialog(MrlLauncher.this,
                        "Selected map is not valid.",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
            }

            if (autoAgentCheckBox.isSelected()) {
                new Thread(new StartAgentsThread()).start();
            }
        });

        stepButton.setText("STEP");
        stepButton.setEnabled(false);

        runButton.setText("RUN");
        runButton.setEnabled(false);

        GroupLayout kernelBPanelLayout = new GroupLayout(kernelBPanel);
        kernelBPanel.setLayout(kernelBPanelLayout);
        kernelBPanelLayout.setHorizontalGroup(
                kernelBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(kernelBPanelLayout.createSequentialGroup()
//                                .addContainerGap()
                                        .addGroup(kernelBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addComponent(reRunButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(loadKernelButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(startAgentsButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(stepButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(runButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
//                                .addContainerGap())
                        )
        );
        kernelBPanelLayout.setVerticalGroup(
                kernelBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, kernelBPanelLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(reRunButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(loadKernelButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(startAgentsButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(stepButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(runButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
//                                        .addComponent(progLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(kernelBPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(mainTabbedPane, GroupLayout.DEFAULT_SIZE, 669, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
//                                .addComponent(progLabel, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(mainTabbedPane)
                                        .addComponent(kernelBPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        setLocationRelativeTo(null);
        addWindowListener(this);
        pack();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    private void mapSelectButtonActionPerformed() {
        String path = mrlKernelConfig.getAbsolutePath();
        String fileSeparator = File.separator;
        if (mapAddressTextField.getText() != null
                && !mapAddressTextField.getText().equalsIgnoreCase("map address")) {
            String tmp = mapAddressTextField.getText();
            if (tmp.contains(fileSeparator)) {
                path = tmp;
            }
        }

        int index = path.lastIndexOf(fileSeparator);
        path = path.substring(0, index);

        JFileChooser fileChooser = new JFileChooser(new File(path));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("select map folder");
        fileChooser.showDialog(this, "select");
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (!f.isDirectory()) {
                    return false;
                }
                String address = f.getAbsolutePath();
                File map = new File(address + File.separator + "map");
                return map.exists() && map.isDirectory();
            }

            @Override
            public String getDescription() {
                return "map directory";
            }
        });
        File file = fileChooser.getSelectedFile();
        if (file != null && file.isDirectory()) {
            String mapPath = file.getPath();
            mapAddressTextField.setText(mapPath);
            loadMapForEdit(editor);
            setConfigFilesPanel();
        }
    }

    private void importMapEditor() {
        JMenuBar menuBar = new JMenuBar();
        editor = new ScenarioEditor(menuBar);
        loadMapForEdit(editor);

        JToolBar fileToolbar = ((JToolBar) ((JPanel) editor.getComponent(1)).getComponent(0));
        JToolBar editToolbar = ((JToolBar) ((JPanel) editor.getComponent(1)).getComponent(1));
        JToolBar functionsToolbar = ((JToolBar) ((JPanel) editor.getComponent(1)).getComponent(2));
        JToolBar toolsToolbar = ((JToolBar) ((JPanel) editor.getComponent(1)).getComponent(3));

        final JSplitPane mapEditorSplitPane = (JSplitPane) editor.getComponent(0);
        JPanel mapEditorTopPanel = new JPanel();
        JPanel mapEditorToolsPanel = getMapEditorToolsPanel(toolsToolbar);
        JLabel mapEditorStatusPanel = (JLabel) editor.getComponent(2);

        mapEditorSplitPane.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                mapEditorSplitPane.setDividerLocation(0.80);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        GroupLayout mapEditorStatusPanelLayout = new GroupLayout(mapEditorStatusPanel);
        mapEditorStatusPanel.setLayout(mapEditorStatusPanelLayout);
        mapEditorStatusPanelLayout.setHorizontalGroup(
                mapEditorStatusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        mapEditorStatusPanelLayout.setVerticalGroup(
                mapEditorStatusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 24, Short.MAX_VALUE)
        );

        fileToolbar.setFloatable(false);
        fileToolbar.setBorder(BorderFactory.createEtchedBorder());
        editToolbar.setFloatable(false);
        editToolbar.setBorder(BorderFactory.createEtchedBorder());
        functionsToolbar.setFloatable(false);
        functionsToolbar.setBorder(BorderFactory.createEtchedBorder());

        GroupLayout mapEditorTopPanelLayout = new GroupLayout(mapEditorTopPanel);
        mapEditorTopPanel.setLayout(mapEditorTopPanelLayout);
        mapEditorTopPanelLayout.setHorizontalGroup(
                mapEditorTopPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorTopPanelLayout.createSequentialGroup()
//                                .addContainerGap()
                                        .addComponent(fileToolbar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(editToolbar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(functionsToolbar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
        );
        mapEditorTopPanelLayout.setVerticalGroup(
                mapEditorTopPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mapEditorTopPanelLayout.createSequentialGroup()
//                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(mapEditorTopPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(fileToolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(editToolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(functionsToolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        )
        );

        GroupLayout mapEditorPanelLayout = new GroupLayout(mapEditorPanel);
        mapEditorPanel.setLayout(mapEditorPanelLayout);
        mapEditorPanelLayout.setHorizontalGroup(
                mapEditorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorPanelLayout.createSequentialGroup()
                                .addGroup(mapEditorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(mapEditorStatusPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(mapEditorTopPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(mapEditorSplitPane, GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(mapEditorToolsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
        mapEditorPanelLayout.setVerticalGroup(
                mapEditorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorPanelLayout.createSequentialGroup()
                                .addComponent(mapEditorTopPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(mapEditorSplitPane, GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(mapEditorStatusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addComponent(mapEditorToolsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }

    private void loadMapForEdit(ScenarioEditor editor) {
        String address = getMapAddress(false);
        if (editor != null && address != null) {
            try {
                editor.load(address + File.separator + "map");
                if (mapConfigTextArea != null) {
                    mapConfigTextArea.setText("");
                    mapConfigTextArea.setBorder(BorderFactory.createTitledBorder("config"));
                }
            } catch (CancelledByUserException | MapException | rescuecore2.scenario.exceptions.ScenarioException | ScenarioException e) {
                Logger.error(e.getMessage());
            }
        }
    }

    private JPanel getMapEditorToolsPanel(JToolBar toolToolbar) {
        final Color CIVILIAN_COLOUR = new Color(0, 255, 0);
        final Color FIRE_BRIGADE_COLOUR = new Color(255, 0, 0);
        final Color POLICE_FORCE_COLOUR = new Color(0, 120, 255);
        final Color AMBULANCE_TEAM_COLOUR = new Color(255, 255, 255);
        final Color FIRE_COLOUR = new Color(255, 0, 0, 128);
        final Color FIRE_STATION_COLOUR = new Color(255, 255, 0);
        final Color POLICE_OFFICE_COLOUR = new Color(0, 120, 255);
        final Color AMBULANCE_CENTRE_COLOUR = new Color(255, 255, 255);
        final Color REFUGE_COLOUR = new Color(0, 128, 0);
        final Color HYDRANT_COLOUR = new Color(128, 128, 0);
        final Color GAS_STATION_COLOUR = new Color(255, 128, 0);
        JPanel mapEditorToolsPanel = new JPanel();

        JToolBar fireToolBar = new JToolBar();
        JToolBar refugeToolBar = new JToolBar();
        JToolBar gasStationToolBar = new JToolBar();
        JToolBar hydrantToolBar = new JToolBar();
        JToolBar civilianToolBar = new JToolBar();
        JToolBar ambulanceTeamToolBar = new JToolBar();
        JToolBar policeForceToolBar = new JToolBar();
        JToolBar fireBrigadeToolBar = new JToolBar();
        JToolBar ambulanceCentreToolBar = new JToolBar();
        JToolBar policeOfficeToolBar = new JToolBar();
        JToolBar fireStationToolBar = new JToolBar();

        fireToolBar.setRollover(true);
        fireToolBar.setFloatable(false);
        fireToolBar.setBorder(BorderFactory.createEtchedBorder());
        fireToolBar.setBackground(FIRE_COLOUR);
        fireToolBar.add(toolToolbar.getComponent(1));
        fireToolBar.addSeparator();
        fireToolBar.add(toolToolbar.getComponent(1));

        refugeToolBar.setRollover(true);
        refugeToolBar.setFloatable(false);
        refugeToolBar.setBorder(BorderFactory.createEtchedBorder());
        refugeToolBar.setBackground(REFUGE_COLOUR);
        refugeToolBar.add(toolToolbar.getComponent(1));
        refugeToolBar.addSeparator();
        refugeToolBar.add(toolToolbar.getComponent(1));

        gasStationToolBar.setRollover(true);
        gasStationToolBar.setFloatable(false);
        gasStationToolBar.setBorder(BorderFactory.createEtchedBorder());
        gasStationToolBar.setBackground(GAS_STATION_COLOUR);
        gasStationToolBar.add(toolToolbar.getComponent(1));
        gasStationToolBar.addSeparator();
        gasStationToolBar.add(toolToolbar.getComponent(1));

        hydrantToolBar.setRollover(true);
        hydrantToolBar.setFloatable(false);
        hydrantToolBar.setBorder(BorderFactory.createEtchedBorder());
        hydrantToolBar.setBackground(HYDRANT_COLOUR);
        hydrantToolBar.add(toolToolbar.getComponent(1));
        hydrantToolBar.addSeparator();
        hydrantToolBar.add(toolToolbar.getComponent(1));

        civilianToolBar.setRollover(true);
        civilianToolBar.setFloatable(false);
        civilianToolBar.setBorder(BorderFactory.createEtchedBorder());
        civilianToolBar.setBackground(CIVILIAN_COLOUR);
        civilianToolBar.add(toolToolbar.getComponent(1));
        civilianToolBar.addSeparator();
        civilianToolBar.add(toolToolbar.getComponent(1));

        fireBrigadeToolBar.setRollover(true);
        fireBrigadeToolBar.setFloatable(false);
        fireBrigadeToolBar.setBorder(BorderFactory.createEtchedBorder());
        fireBrigadeToolBar.setBackground(FIRE_BRIGADE_COLOUR);
        fireBrigadeToolBar.add(toolToolbar.getComponent(2));
        fireBrigadeToolBar.addSeparator();
        fireBrigadeToolBar.add(toolToolbar.getComponent(2));

        policeForceToolBar.setRollover(true);
        policeForceToolBar.setFloatable(false);
        policeForceToolBar.setBorder(BorderFactory.createEtchedBorder());
        policeForceToolBar.setBackground(POLICE_FORCE_COLOUR);
        policeForceToolBar.add(toolToolbar.getComponent(2));
        policeForceToolBar.addSeparator();
        policeForceToolBar.add(toolToolbar.getComponent(2));

        ambulanceTeamToolBar.setRollover(true);
        ambulanceTeamToolBar.setFloatable(false);
        ambulanceTeamToolBar.setBorder(BorderFactory.createEtchedBorder());
        ambulanceTeamToolBar.setBackground(AMBULANCE_TEAM_COLOUR);
        ambulanceTeamToolBar.add(toolToolbar.getComponent(2));
        ambulanceTeamToolBar.addSeparator();
        ambulanceTeamToolBar.add(toolToolbar.getComponent(2));

        fireStationToolBar.setRollover(true);
        fireStationToolBar.setFloatable(false);
        fireStationToolBar.setBorder(BorderFactory.createEtchedBorder());
        fireStationToolBar.setBackground(FIRE_STATION_COLOUR);
        fireStationToolBar.add(toolToolbar.getComponent(3));
        fireStationToolBar.addSeparator();
        fireStationToolBar.add(toolToolbar.getComponent(3));

        policeOfficeToolBar.setRollover(true);
        policeOfficeToolBar.setFloatable(false);
        policeOfficeToolBar.setBorder(BorderFactory.createEtchedBorder());
        policeOfficeToolBar.setBackground(POLICE_OFFICE_COLOUR);
        policeOfficeToolBar.add(toolToolbar.getComponent(3));
        policeOfficeToolBar.addSeparator();
        policeOfficeToolBar.add(toolToolbar.getComponent(3));

        ambulanceCentreToolBar.setRollover(true);
        ambulanceCentreToolBar.setFloatable(false);
        ambulanceCentreToolBar.setBorder(BorderFactory.createEtchedBorder());
        ambulanceCentreToolBar.setBackground(AMBULANCE_CENTRE_COLOUR);
        ambulanceCentreToolBar.add(toolToolbar.getComponent(3));
        ambulanceCentreToolBar.addSeparator();
        ambulanceCentreToolBar.add(toolToolbar.getComponent(3));

        GroupLayout mapEditorToolsPanelLayout = new GroupLayout(mapEditorToolsPanel);
        mapEditorToolsPanel.setLayout(mapEditorToolsPanelLayout);
        mapEditorToolsPanelLayout.setHorizontalGroup(
                mapEditorToolsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(fireToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(refugeToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(hydrantToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(gasStationToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(civilianToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ambulanceTeamToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(policeForceToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fireBrigadeToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ambulanceCentreToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(policeOfficeToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fireStationToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mapEditorToolsPanelLayout.setVerticalGroup(
                mapEditorToolsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorToolsPanelLayout.createSequentialGroup()
                                .addContainerGap(0, Short.MAX_VALUE)
                                .addComponent(fireToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(refugeToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(gasStationToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(hydrantToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(civilianToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(ambulanceTeamToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(policeForceToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fireBrigadeToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(ambulanceCentreToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(policeOfficeToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fireStationToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(0, Short.MAX_VALUE))
        );

        return mapEditorToolsPanel;
    }

    private boolean pressedCtr = false;

    private void importMapConfigEditor() {

        JPanel fileToolbar = getFileToolbar();
//        JPanel configsPanel = getConfigPanel();
        setConfigFilesPanel();
        JScrollPane textPanel = new JScrollPane();
        mapConfigTextArea = new JTextArea();
        mapConfigTextArea.setColumns(20);
        mapConfigTextArea.setRows(20);
        mapConfigTextArea.setFont(new Font("Arial", Font.PLAIN, 13));
        mapConfigTextArea.setBorder(BorderFactory.createTitledBorder("config"));
        mapConfigTextArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 17) {
                    pressedCtr = true;
                }
                if (e.getKeyCode() == 83 && pressedCtr) {
                    save();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 17) {
                    pressedCtr = false;
                }
            }
        });
        textPanel.setViewportView(mapConfigTextArea);
//        mapConfigTextArea.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                if (saveFile != null) {
//                    mapConfigTextArea.setBorder(BorderFactory.createTitledBorder(saveFile.getName() + " *"));
//                }
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//            }
//        });

        GroupLayout mapEditorPanelLayout = new GroupLayout(mapConfigEditorPanel);
        mapConfigEditorPanel.setLayout(mapEditorPanelLayout);
        mapEditorPanelLayout.setHorizontalGroup(
                mapEditorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorPanelLayout.createSequentialGroup()
                                .addGroup(mapEditorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(fileToolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(textPanel, GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(configsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
        mapEditorPanelLayout.setVerticalGroup(
                mapEditorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorPanelLayout.createSequentialGroup()
                                .addComponent(fileToolbar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(textPanel, GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE))
                        .addComponent(configsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

    }

    private void setConfigFilesPanel() {
        JPanel configsPanel1 = getConfigPanelNew();
        configsPanel.setViewportView(configsPanel1);
    }

    private JPanel getFileToolbar() {
        Action saveAction = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        };
        Action saveAsAction = new AbstractAction("Save as") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        };

        JToolBar toolbar = new JToolBar();
        toolbar.setRollover(true);
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEtchedBorder());
        toolbar.add(saveAction);
        toolbar.addSeparator();
        toolbar.add(saveAsAction);

        JPanel mapConfigEditorTopPanel = new JPanel();
        GroupLayout mapEditorTopPanelLayout = new GroupLayout(mapConfigEditorTopPanel);
        mapConfigEditorTopPanel.setLayout(mapEditorTopPanelLayout);
        mapEditorTopPanelLayout.setHorizontalGroup(
                mapEditorTopPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mapEditorTopPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(toolbar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
        );
        mapEditorTopPanelLayout.setVerticalGroup(
                mapEditorTopPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mapEditorTopPanelLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(mapEditorTopPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(toolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        )
        );


        return mapConfigEditorTopPanel;
    }

    private void load(String fileName) {
        FileReader fr = null;
        BufferedReader br = null;
        try {
            mapConfigTextArea.setBorder(BorderFactory.createTitledBorder(fileName));
            mapConfigTextArea.setText("");
            String address = getMapAddress(false);
            if (address != null) {

                File f = new File(address + File.separator + "config" + File.separator + fileName);
                fr = new FileReader(f);
                br = new BufferedReader(fr);
                String p = br.readLine();
                while (p != null) {
                    mapConfigTextArea.append(p + "\n");
                    p = br.readLine();
                }
                saveFile = f;

            }
        } catch (FileNotFoundException e) {
            mapConfigTextArea.setText("file not found");
        } catch (IOException e) {
            Logger.error(e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Logger.error(e.getMessage());
                }
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    Logger.error(e.getMessage());
                }
            }
        }
    }

    private void save() {
        if (saveFile == null) {
            saveAs();
        }
        if (saveFile != null) {
            FileWriter wr = null;
            try {
                if (!saveFile.exists()) {
                    File parent = saveFile.getParentFile();
                    if (!parent.exists()) {
                        if (!saveFile.getParentFile().mkdirs()) {
                            Logger.error("Couldn't create file " + saveFile.getPath());
                        }
                    }
                    if (!saveFile.createNewFile()) {
                        Logger.error("Couldn't create file " + saveFile.getPath());
                    }
                }
                wr = new FileWriter(saveFile);
                wr.write(mapConfigTextArea.getText());

            } catch (IOException e) {
                Logger.error(e.getMessage());
            } finally {
                if (wr != null) {
                    try {
                        wr.close();
                    } catch (IOException e) {
                        Logger.error(e.getMessage());
                    }
                }
            }
        }
    }

    private void saveAs() {
        JFileChooser chooser = new JFileChooser();
        String address = getMapAddress(false);
        if (address != null) {
            chooser.setCurrentDirectory(new File(address + File.separator + "config"));
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            saveFile = chooser.getSelectedFile();
            save();
        }
    }

    private JPanel getConfigPanelNew() {
        JPanel configsPanel = new JPanel();
        GroupLayout mapEditorToolsPanelLayout = new GroupLayout(configsPanel);
        configsPanel.setLayout(mapEditorToolsPanelLayout);

        GroupLayout.ParallelGroup horizontalGroup = mapEditorToolsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        mapEditorToolsPanelLayout.setHorizontalGroup(horizontalGroup);

        GroupLayout.SequentialGroup sequentialGroup = mapEditorToolsPanelLayout.createSequentialGroup();
        GroupLayout.ParallelGroup verticalGroup = mapEditorToolsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(sequentialGroup);
        mapEditorToolsPanelLayout.setVerticalGroup(verticalGroup);

        String address = getMapAddress(false);

        if (address != null) {
            File mainFolder = new File(address + File.separator + "config" + File.separator);
            if (mainFolder.isDirectory()) {
                File[] files = mainFolder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            JToolBar toolBar = getToolBar(f.getName(), "");
                            horizontalGroup.addComponent(toolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
                            sequentialGroup.addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
                        } else if (!f.getName().equalsIgnoreCase(".svn")) {
                            Random random = new Random(System.nanoTime());
                            Color color = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
                            File[] files1 = f.listFiles();
                            if (files1 != null) {
                                for (File f2 : files1) {
                                    if (f2.isFile()) {
                                        JToolBar toolBar = getToolBar(f2.getName(), f.getName() + File.separator);
                                        toolBar.setBackground(color);
                                        horizontalGroup.addComponent(toolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
                                        sequentialGroup.addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
                                    }
                                }
                            }
                        }
                    }
                }
                mapEditorToolsPanelLayout.setHorizontalGroup(horizontalGroup);
                mapEditorToolsPanelLayout.setVerticalGroup(verticalGroup);
                configsPanel.setLayout(mapEditorToolsPanelLayout);
            }
        }
        return configsPanel;
    }

    private JToolBar getToolBar(String name, String folder) {
        final String fileName = folder + name;
        JToolBar toolBar = new JToolBar();
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEtchedBorder());
        toolBar.add(new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                load(fileName);
            }
        });
        return toolBar;
    }

    private class LoadKernelThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "kernel.cfg");
            arguments.add("--gis.map.dir=" + address + File.separator + "map");
            arguments.add("--nomenu");
            if (autoAgentCheckBox.isSelected()) {
                arguments.add(LaunchKernel.AUTORUN);
            }

            String[] kernelArgs = new String[arguments.size()];
            kernelArgs = arguments.toArray(kernelArgs);

            startKernel(kernelArgs);
            if (!autoAgentCheckBox.isSelected()) {
                runButton.setEnabled(true);
                stepButton.setEnabled(true);
                startAgentsButton.setEnabled(true);
            }
            System.out.println("Kernel is ready.");

            String hostField = hostTextField.getText();
            String portField = null;
            arguments.add(hostField != null ? hostField : defHostAddress);

            try {
                portField = Integer.valueOf(portTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            if (showViewerCheckBox.isSelected()) {
                try {
                    loadVisualDebugger(hostField, portField, kernelArgs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            mainTabbedPane.setSelectedIndex(2);
        }
    }

    private class StartAgentsThread implements Runnable {

        @Override
        public void run() {
            if (autoAgentCheckBox.isSelected()) {
                while (!wait_for_agents) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            String hostField = hostTextField.getText();
            String portField = null;
            String atField = null;
            String acField = null;
            String fbField = null;
            String fsField = null;
            String pfField = null;
            String poField = null;
            try {
                portField = Integer.valueOf(portTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            try {
                atField = Integer.valueOf(ATTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            try {
                acField = Integer.valueOf(ACTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            try {
                fbField = Integer.valueOf(FBTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            try {
                fsField = Integer.valueOf(FSTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            try {
                pfField = Integer.valueOf(PFTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }
            try {
                poField = Integer.valueOf(POTextField.getText()).toString();
            } catch (NumberFormatException ignore) {
            }

            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
//            arguments.add("-fb");
            arguments.add(fbField != null ? fbField : "-1");
//            arguments.add("-fs");
            arguments.add(fsField != null ? fsField : "-1");
//            arguments.add("-pf");
            arguments.add(pfField != null ? pfField : "-1");
//            arguments.add("-po");
            arguments.add(poField != null ? poField : "-1");
//            arguments.add("-at");
            arguments.add(atField != null ? atField : "-1");
//            arguments.add("-ac");
            arguments.add(acField != null ? acField : "-1");
//            arguments.add("-h");
            arguments.add(hostField != null ? hostField : defHostAddress);
//            arguments.add("-p");
//            arguments.add(portField != null ? portField : "7000");
            boolean precompute = preComputeCheckBox.isSelected();
//            if (precompute) {
//                arguments.add("-precompute");
//            }
//            if (onThreadCheckBox.isSelected()) {
//                arguments.add("-thr");
//            }

            String otherArgs = agentParamsTextField.getText();
            if (otherArgs.length() > 0) {
                String[] oa = otherArgs.split(" ");
                Collections.addAll(arguments, oa);
            }

            String[] agentArgs = new String[arguments.size()];
            agentArgs = arguments.toArray(agentArgs);
            //-h 127.0.0.1 -p 7000
            try {
                // launch agents
                launchAgents(agentArgs, precompute);

                //launch mrlViewer

                if (!autoAgentCheckBox.isSelected()) {
                    stepButton.setEnabled(true);
                    runButton.setEnabled(true);
                }
                agents_connected = true;
                if (showViewerCheckBox.isSelected()) {
                    mainTabbedPane.setSelectedIndex(4);
                } else {
                    mainTabbedPane.setSelectedIndex(3);
                }
            } catch (Exception e) {
                //Logger.error(e.getMessage());
                Logger.info("failed: " + e.getMessage());
            }
        }
    }

    private class LoadMiscSimulatorThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("misc.MiscSimulator");
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "misc.cfg");
            arguments.add("--nogui");

            String[] args = new String[arguments.size()];
            args = arguments.toArray(args);

            LaunchComponents.main(args);
            System.out.println("Misc simulator is ready.");
        }
    }

    private class LoadTrafficSimulatorThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("traffic3.simulator.TrafficSimulator");
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "traffic3.cfg");
            arguments.add("--nogui");

            String[] args = new String[arguments.size()];
            args = arguments.toArray(args);

            LaunchComponents.main(args);
            System.out.println("Traffic simulator is ready.");
        }
    }

    private class LoadFireSimulatorThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("firesimulator.FireSimulatorWrapper");
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "resq-fire.cfg");
            arguments.add("--nogui");

            String[] args = new String[arguments.size()];
            args = arguments.toArray(args);

            LaunchComponents.main(args);
            System.out.println("Fire simulator is ready.");
        }
    }

    private class LoadIgnitionSimulatorThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("ignition.IgnitionSimulator");
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "ignition.cfg");
            arguments.add("--nogui");

            String[] args = new String[arguments.size()];
            args = arguments.toArray(args);

            LaunchComponents.main(args);
            System.out.println("Ignition simulator is ready.");
        }
    }

    private class LoadCollapseSimulatorThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("collapse.CollapseSimulator");
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "collapse.cfg");
            arguments.add("--nogui");

            String[] args = new String[arguments.size()];
            args = arguments.toArray(args);

            LaunchComponents.main(args);
            System.out.println("Collapse simulator is ready.");
        }
    }

    private class LoadClearSimulatorThread implements Runnable {

        @Override
        public void run() {
            List<String> arguments = new ArrayList<>();
            String address = getMapAddress(false);
            if (address == null) {
                return;
            }
            arguments.add("clear.ClearSimulator");
            arguments.add("-c");
            arguments.add(address + File.separator + "config" + File.separator + "clear.cfg");
            arguments.add("--nogui");

            String[] args = new String[arguments.size()];
            args = arguments.toArray(args);

            LaunchComponents.main(args);
            System.out.println("Clear simulator is ready.");
        }
    }

    private void loadVisualDebugger(String hostField, String portField, String[] agentArgs) throws IOException, ConfigException, InterruptedException, ConnectionException, ComponentConnectionException {
        Config config = new Config();
        agentArgs = CommandLineOptions.processArgs(agentArgs, config);
        int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
        String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);
        ComponentLauncher launcher = new TCPComponentLauncher(hostField, Integer.valueOf(portField), config);
        MrlViewer mrlViewer = new MrlViewer(true);
        launcher.connect(mrlViewer);

        JPanel topPanel = new JPanel();//mrlViewer.getTopPanel();
        JLabel timeLabel = mrlViewer.getTimeLabel();
        JLabel scoreLabel = mrlViewer.getScoreLabel();
        JLabel mapNameLabel = new JLabel("map address: " + mapAddressTextField.getText());
        try {
            LaunchMRLViewer.listenToAgents(mrlViewer);
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
        MrlAnimatedWorldModelViewer mrlViewerPanel = mrlViewer.getViewerPanel();
        JScrollPane agentPropsScrollPane = new JScrollPane();
        JScrollPane layersScrollPane = new JScrollPane();
        JTable agentPropsTable = mrlViewer.getAgentPropPanel();
        JComboBox<StandardEntity> agentComboBox = mrlViewer.getAgentSelPanel();
        JTree mrlLayerConPanel = mrlViewer.getLayerConPanel();
        JPanel bottomPanel = mrlViewer.getBottomPanel();

        GroupLayout topPanelLayout = new GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
                topPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(topPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scoreLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 170, Short.MAX_VALUE)
                                .addComponent(timeLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 170, Short.MAX_VALUE)
                                .addComponent(mapNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        topPanelLayout.setVerticalGroup(
                topPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, topPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(scoreLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(timeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(mapNameLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        GroupLayout mrlViewerPanelLayout = new GroupLayout(mrlViewerPanel);
        mrlViewerPanel.setLayout(mrlViewerPanelLayout);
        mrlViewerPanelLayout.setHorizontalGroup(
                mrlViewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        );
        mrlViewerPanelLayout.setVerticalGroup(
                mrlViewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        );

        GroupLayout mrlLayerConPanelLayout = new GroupLayout(mrlLayerConPanel);
        mrlLayerConPanel.setLayout(mrlLayerConPanelLayout);
        mrlLayerConPanelLayout.setHorizontalGroup(
                mrlLayerConPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        );
        mrlLayerConPanelLayout.setVerticalGroup(
                mrlLayerConPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        );

        agentPropsScrollPane.setViewportView(agentPropsTable);
        layersScrollPane.setViewportView(mrlLayerConPanel);

        GroupLayout bottomPanelPanelLayout = new GroupLayout(bottomPanel);
        bottomPanel.setLayout(bottomPanelPanelLayout);
        bottomPanelPanelLayout.setHorizontalGroup(
                bottomPanelPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        );
        bottomPanelPanelLayout.setVerticalGroup(
                bottomPanelPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        );

        GroupLayout viewerPanelLayout = new GroupLayout(viewerPanel);
        viewerPanel.setLayout(viewerPanelLayout);
        viewerPanelLayout.setHorizontalGroup(
                viewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(topPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bottomPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(viewerPanelLayout.createSequentialGroup()
                                .addComponent(mrlViewerPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(viewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(layersScrollPane)
                                        .addComponent(agentPropsScrollPane, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(agentComboBox, 0, 200, Short.MAX_VALUE)))
        );
        viewerPanelLayout.setVerticalGroup(
                viewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(viewerPanelLayout.createSequentialGroup()
                                .addComponent(topPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(viewerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(mrlViewerPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(viewerPanelLayout.createSequentialGroup()
                                                .addGap(6, 6, 6)
                                                .addComponent(agentComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(agentPropsScrollPane, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(layersScrollPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bottomPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
    }


    private String getMapAddress(boolean projectFolder) {
        if (projectFolder) {
            return mrlKernelConfig.getAbsolutePath();
        }

        String address = mapAddressTextField.getText();
        if (!address.equalsIgnoreCase("map address")) {
//            address = address.replaceAll("\\\\", "/");
            File f = new File(address + File.separator + "map");
            if (f.exists() && f.isDirectory()) {
                return address;
            }
        }
        return null;
    }

    private void startKernel(String[] args) {
        Config config = new Config();
        boolean showStartupMenu = true;
        boolean showGUI = true;
        boolean autorun = false;
//        Logger.setLogContext("startup");
        try {
            File rays = new File("rays");
            if (!rays.exists()) {
                rays.mkdir();
            }
            args = CommandLineOptions.processArgs(args, config);
            for (String arg : args) {
                if (arg.equalsIgnoreCase(LaunchKernel.NO_GUI)) {
                    showGUI = false;
                } else if (arg.equalsIgnoreCase(LaunchKernel.NO_STARTUP_MENU)) {
                    showStartupMenu = false;
                } else if (arg.equalsIgnoreCase(LaunchKernel.AUTORUN)) {
                    autorun = true;
                } else {
                    Logger.warn("Unrecognised option: " + arg);
                }
            }
            // Process jar files
            LaunchKernel.processJarFiles(config);
            Registry localRegistry = new Registry("Kernel local registry");
            // Register preferred message, entity and property factories
            for (String next : config.getArrayValue(Constants.MESSAGE_FACTORY_KEY, "")) {
                MessageFactory factory = instantiateFactory(next, MessageFactory.class);
                if (factory != null) {
                    localRegistry.registerMessageFactory(factory);
                    Logger.info("Registered local message factory: " + next);
                }
            }
            for (String next : config.getArrayValue(Constants.ENTITY_FACTORY_KEY, "")) {
                EntityFactory factory = instantiateFactory(next, EntityFactory.class);
                if (factory != null) {
                    localRegistry.registerEntityFactory(factory);
                    Logger.info("Registered local entity factory: " + next);
                }
            }
            for (String next : config.getArrayValue(Constants.PROPERTY_FACTORY_KEY, "")) {
                PropertyFactory factory = instantiateFactory(next, PropertyFactory.class);
                if (factory != null) {
                    localRegistry.registerPropertyFactory(factory);
                    Logger.info("Registered local property factory: " + next);
                }
            }
            // CHECKSTYLE:OFF:MagicNumber
            config.addConstraint(new IntegerValueConstraint(Constants.KERNEL_PORT_NUMBER_KEY, 1, 65535));
            // CHECKSTYLE:ON:MagicNumber
            config.addConstraint(new IntegerValueConstraint(LaunchKernel.KERNEL_STARTUP_TIME_KEY, 0, Integer.MAX_VALUE));
            config.addConstraint(new ClassNameSetValueConstraint(Constants.MESSAGE_FACTORY_KEY, MessageFactory.class));
            config.addConstraint(new ClassNameSetValueConstraint(Constants.ENTITY_FACTORY_KEY, EntityFactory.class));
            config.addConstraint(new ClassNameSetValueConstraint(Constants.PROPERTY_FACTORY_KEY, PropertyFactory.class));
            config.addConstraint(new ClassNameSetValueConstraint(LaunchKernel.COMMAND_FILTERS_KEY, CommandFilter.class));
            config.addConstraint(new ClassNameSetValueConstraint(LaunchKernel.TERMINATION_KEY, TerminationCondition.class));
            config.addConstraint(new ClassNameSetValueConstraint(LaunchKernel.COMMAND_COLLECTOR_KEY, CommandCollector.class));
            config.addConstraint(new ClassNameSetValueConstraint(LaunchKernel.GUI_COMPONENTS_KEY, GUIComponent.class));
            config.addConstraint(new ClassNameValueConstraint(LaunchKernel.AGENT_REGISTRAR_KEY, AgentRegistrar.class));
            config.addConstraint(new ClassNameValueConstraint(Constants.SCORE_FUNCTION_KEY, ScoreFunction.class));

            //Logger.setLogContext("kernel");
            kernelInfo = LaunchKernel.createKernel(config, showStartupMenu);
            if (kernelInfo == null) {
                Logger.error("kernelInfo is null");
                return;
            }
            KernelGUI gui = null;
            if (showGUI) {
                gui = new KernelGUI(kernelInfo.kernel, kernelInfo.componentManager, config, localRegistry, !autorun);
                for (GUIComponent next : kernelInfo.guiComponents) {
                    gui.addGUIComponent(next);
                    if (next instanceof KernelListener) {
                        kernelInfo.kernel.addKernelListener((KernelListener) next);
                    }
                }

                //add kernel gui into mrl kernel launcher
                GroupLayout kernelPanelLayout = new GroupLayout(kernelPanel);
                kernelPanel.setLayout(kernelPanelLayout);
                kernelPanelLayout.setHorizontalGroup(
                        kernelPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(kernelPanelLayout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(gui)
                                        .addContainerGap())
                );
                kernelPanelLayout.setVerticalGroup(
                        kernelPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(kernelPanelLayout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(gui)
                                        .addContainerGap())
                );

                if (!autoAgentCheckBox.isSelected()) {
                    JButton kernelStepButton = (JButton) ((JPanel) gui.getComponent(2)).getComponent(6);
                    JButton kernelRunButton = (JButton) ((JPanel) gui.getComponent(2)).getComponent(7);
                    stepButton.setText("STEP");
                    stepButton.setEnabled(false);
                    stepButton.addActionListener(kernelStepButton.getActionListeners()[0]);
                    stepButton.addActionListener(e -> startAgentsButton.setEnabled(false));
                    kernelStepButton.addComponentListener(new ComponentListener() {
                        @Override
                        public void componentResized(ComponentEvent e) {
                            System.out.println("componentResized" + e);
                        }

                        @Override
                        public void componentMoved(ComponentEvent e) {
                            System.out.println("componentMoved" + e);
                        }

                        @Override
                        public void componentShown(ComponentEvent e) {
                            System.out.println("componentShown" + e);
                        }

                        @Override
                        public void componentHidden(ComponentEvent e) {
                            System.out.println("componentHidden" + e);
                        }
                    });
                    runButton.setText("RUN");
                    runButton.setEnabled(false);
                    runButton.addActionListener(kernelRunButton.getActionListeners()[0]);
                    runButton.addActionListener(e -> {

                        if (stepButton.isEnabled()) {
                            runButton.setText("STOP");
                            stepButton.setEnabled(false);
                            startAgentsButton.setEnabled(false);
                        } else {
                            runButton.setText("RUN");
                            stepButton.setEnabled(true);
                        }
                    });

                    // remove kernel default controls
                    ((JPanel) gui.getComponent(2)).removeAll();
                }
            }
            LaunchKernel.initialiseKernel(kernelInfo, config, localRegistry);
            LaunchKernel.autostartComponents(kernelInfo, localRegistry, gui, config);
            if (!showGUI || autorun) {
                wait_for_agents = true;
                LaunchKernel.waitForComponentManager(kernelInfo, config);
                if (autoAgentCheckBox.isSelected()) {
                    while (!agents_connected) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
                Kernel kernel = kernelInfo.kernel;
                while (!kernel.hasTerminated()) {
                    kernel.timestep();
                }
                kernel.shutdown();
            }
        } catch (ConfigException | IOException | KernelException e) {
            Logger.fatal("Couldn't start kernel", e);
        } catch (LogException e) {
            Logger.fatal("Couldn't write log", e);
        } catch (InterruptedException e) {
            Logger.fatal("Kernel interrupted");
        } catch (DocumentException e) {
            Logger.fatal("Document Exception");
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                MrlLauncher mrlLauncher = new MrlLauncher();
                mrlLauncher.setFont(new Font("Vani", Font.BOLD, 12)); // NOI18N
                mrlLauncher.setTitle("Control Panel");
//                ImageIcon imageIcon = new ImageIcon("icon.png");
//                mrlLauncher.setIconImage(imageIcon.getImage());
                mrlLauncher.setExtendedState(MAXIMIZED_BOTH);
                mrlLauncher.setVisible(true);
            } catch (Exception ex) {
                Logger.error(ex.getMessage());
            }

        });
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        saveConfigFile();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        saveConfigFile();
    }

    private void saveConfigFile() {
        String mapAddress = mapAddressTextField.getText();
        boolean autoRunAgentSelected = autoAgentCheckBox.isSelected();
        String hostAddress = hostTextField.getText();
        String portAddress = portTextField.getText();
        boolean multipleRun = multipleRunCheckBox.isSelected();
        int runNo = (Integer) runNoSpinner.getValue();
        boolean showMrlViewer = showViewerCheckBox.isSelected();
        String agentOtherParam = agentParamsTextField.getText();
        String atParam = ATTextField.getText();
        String acParam = ACTextField.getText();
        String fbParam = FBTextField.getText();
        String fsParam = FSTextField.getText();
        String pfParam = PFTextField.getText();
        String poParam = POTextField.getText();
        boolean preCompute = preComputeCheckBox.isSelected();
        boolean onThread = onThreadCheckBox.isSelected();
        boolean showKernel = showKernelCheckBox.isSelected();

        FileWriter wr = null;
        try {
            if (!mrlKernelConfig.exists()) {
                if (!mrlKernelConfig.createNewFile()) {
                    Logger.error("Couldn't create file " + mrlKernelConfig.getPath());
                }
            }
            wr = new FileWriter(mrlKernelConfig);
            wr.write("");
            wr.append(defMapAddressString).append(":").append(mapAddress).append("\n");
            wr.append(defAutoRunAgentSelectedString).append(":").append(String.valueOf(autoRunAgentSelected)).append("\n");
            wr.append(defHostAddressString).append(":").append(hostAddress).append("\n");
            wr.append(defPortAddressString).append(":").append(portAddress).append("\n");
            wr.append(defMultipleRunString).append(":").append(String.valueOf(multipleRun)).append("\n");
            wr.append(defRunNoString).append(":").append(String.valueOf(runNo)).append("\n");
            wr.append(defShowMrlViewerString).append(":").append(String.valueOf(showMrlViewer)).append("\n");
            wr.append(defAgentOtherParamString).append(":").append(agentOtherParam).append("\n");
            wr.append(defATParamString).append(":").append(atParam).append("\n");
            wr.append(defACParamString).append(":").append(acParam).append("\n");
            wr.append(defFBParamString).append(":").append(fbParam).append("\n");
            wr.append(defFSParamString).append(":").append(fsParam).append("\n");
            wr.append(defPFParamString).append(":").append(pfParam).append("\n");
            wr.append(defPOParamString).append(":").append(poParam).append("\n");
            wr.append(defPreComputeString).append(":").append(String.valueOf(preCompute)).append("\n");
            wr.append(defOnThreadString).append(":").append(String.valueOf(onThread)).append("\n");
            wr.append(defShowKernelString).append(":").append(String.valueOf(showKernel));
        } catch (IOException ex) {
            Logger.error(ex.getMessage());
        } finally {
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException ex) {
                    Logger.error(ex.getMessage());
                }
            }
        }
    }


}