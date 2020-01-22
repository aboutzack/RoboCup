package AIT_2019.centralized;

import adf.agent.info.*;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.module.ModuleManager;
import adf.agent.action.Action;
import adf.agent.action.common.*;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.*;
import adf.component.centralized.CommandExecutor;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.*;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import rescuecore2.worldmodel.EntityID;
import java.util.*;

public class AITCommandExecutorPolice extends CommandExecutor<CommandPolice>
{
    private static final int ACTION_UNKNOWN = -1;

    private EntityID target;
    private int type;
    private EntityID commander;

    private ExtAction extaction;
    private PathPlanning pathPlanning;

    public AITCommandExecutorPolice(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.type = ACTION_UNKNOWN;

        this.extaction = mm.getExtAction(
            "CommandExecutorPolice.ActionExtClear",
            "adf.sample.extaction.ActionExtClear");

        this.pathPlanning = mm.getModule(
            "CommandExecutorPolice.PathPlanning",
            "adf.sample.module.algorithm.SamplePathPlanning");
    }

    @Override
    public CommandExecutor setCommand(CommandPolice command)
    {
        final EntityID me = this.agentInfo.getID();
        if (!command.isToIDDefined()) return this;
        if (!me.equals(command.getToID())) return this;


        this.target = command.getTargetID();
        this.type = command.getAction();
        this.commander = command.getSenderID();
        return this;
    }

    @Override
    public CommandExecutor precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        if(this.getCountPrecompute() >= 2) return this;

        this.extaction.precompute(pd);
        this.pathPlanning.precompute(pd);
        return this;
    }

    @Override
    public CommandExecutor resume(PrecomputeData pd)
    {
        super.resume(pd);
        if(this.getCountResume() >= 2) return this;

        this.extaction.resume(pd);
        this.pathPlanning.resume(pd);
        return this;
    }

    @Override
    public CommandExecutor preparate()
    {
        super.preparate();
        if(this.getCountPreparate() >= 2) return this;

        this.extaction.preparate();
        this.pathPlanning.preparate();
        return this;
    }

    @Override
    public CommandExecutor updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        if(this.getCountUpdateInfo() >= 2) return this;

        this.extaction.updateInfo(mm);
        this.pathPlanning.updateInfo(mm);

        if(this.target != null && this.isCommandCompleted())
        {
            mm.addMessage(new MessageReport(true, true, true, this.target));
            this.target = null;
            this.type = ACTION_UNKNOWN;
            this.commander = null;
        }

        return this;
    }

    @Override
    public CommandExecutor calc()
    {
        this.result = null;
        if (this.target == null) return this;
        if (this.type == ACTION_UNKNOWN) return this;

        this.extaction.setTarget(this.target);
        this.extaction.calc();
        this.result = this.extaction.getAction();
        return this;
    }

    private boolean isCommandCompleted()
    {
        if (this.needIdle()) return false;

        this.extaction.setTarget(this.target);
        this.extaction.calc();
        final Action action = this.extaction.getAction();
        return this.isEmptyAction(action);
    }

    private boolean isEmptyAction(Action action)
    {
        if (action == null) return true;
        if (action instanceof ActionRest) return true;
        if (action instanceof ActionMove)
        {
            final ActionMove move = (ActionMove)action;
            final int ax = (int)this.agentInfo.getX();
            final int ay = (int)this.agentInfo.getY();
            final int mx = move.getPosX();
            final int my = move.getPosY();
            return ax == mx && ay == my;
        }
        return false;
    }

    private boolean needIdle()
    {
        final int time = this.agentInfo.getTime();
        final int ignored = this.scenarioInfo.getKernelAgentsIgnoreuntil();
        return time < ignored;
    }
}
