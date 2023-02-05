package teamcode.v1.opmodes

import com.asiankoala.koawalib.command.KOpMode
import com.asiankoala.koawalib.command.commands.*
import com.asiankoala.koawalib.command.group.SequentialGroup
import com.asiankoala.koawalib.logger.Logger
import com.asiankoala.koawalib.logger.LoggerConfig
import com.asiankoala.koawalib.math.Pose
import com.asiankoala.koawalib.math.Vector
import com.asiankoala.koawalib.math.radians
import com.asiankoala.koawalib.path.*
import com.asiankoala.koawalib.path.gvf.SimpleGVFController
import com.asiankoala.koawalib.util.OpModeState
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import teamcode.v1.auto.AutoRobot
import teamcode.v1.commands.sequences.AutoDepositSequence
import teamcode.v1.commands.sequences.DepositSequence
import teamcode.v1.commands.sequences.HomeSequence
import teamcode.v1.commands.subsystems.ClawCmds
import teamcode.v1.commands.subsystems.GuideCmds
import teamcode.v1.constants.*
import teamcode.v1.vision.Enums

@Autonomous(preselectTeleOp = "KTeleOp")
class Left : KOpMode() {
    private val robot by lazy { AutoRobot(startPose) }

    private val startPose = Pose(-66.0, 40.0, 180.0.radians)

    private lateinit var mainCommand: Cmd

    private val path1 = HermitePath(
        FLIPPED_HEADING_CONTROLLER,
        Pose(startPose.x, startPose.y, 0.0),
        Pose(-11.0, 31.5, 310.0.radians)
    )

    private val intakePath1 = HermitePath(
        DEFAULT_HEADING_CONTROLLER,
        Pose(-11.0, 31.5, 120.0.radians),
        Pose(-15.0, 59.75, 90.0.radians)
    )

    private val intakePath2 = HermitePath(
        DEFAULT_HEADING_CONTROLLER,
        Pose(-15.0, 31.5, 90.0.radians),
        Pose(-14.0, 59.75, 90.0.radians)
    )

    private val depositPath = HermitePath(
        FLIPPED_HEADING_CONTROLLER,
        Pose(-14.0, 59.0, 270.0.radians),
        Pose(-14.0, 53.0, 270.0.radians),
        Pose(-25.0, 34.0, 210.0.radians)
    )

    private val leftPath = HermitePath(
        DEFAULT_HEADING_CONTROLLER,
        Pose(-14.0, 59.75, 90.0.radians)
    )

    private val middlePath = HermitePath(
        DEFAULT_HEADING_CONTROLLER,
        Pose(-15.0, 31.5, 90.0.radians),
    )

    private val rightPath = HermitePath(
        FLIPPED_HEADING_CONTROLLER,
        Pose(-15.0, 20.0, 270.0.radians)
    )

    override fun mInit() {
        robot.vision.start()
        robot.claw.setPos(ClawConstants.closePos)
        Logger.config = LoggerConfig(
            isLogging = true,
            false,
            isDashboardEnabled = true,
            isTelemetryEnabled = true
        )

        mainCommand = SequentialGroup(
            WaitUntilCmd {opModeState == OpModeState.START},
            InstantCmd({robot.whacker.setPos(WhackerConstants.leftPos)}),
            InstantCmd({robot.lift.setPos(7.0)}),
            GVFCmd(
                robot.drive,
                SimpleGVFController(path1, 0.6, 20.0, 12.0, 0.6, 1.5, 1.5),
                Pair(
                    AutoDepositSequence(robot.lift, robot.arm, robot.claw, robot.guide, robot.whacker, 145.0, LiftConstants.highPos, GuideConstants.depositPos, WhackerConstants.rightPos), ProjQuery(
                        Vector(-60.0, 40.0)
                    )
                )
            ),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            WaitCmd(0.25),
            HomeSequence(robot.lift, robot.claw, robot.arm, robot.guide, ArmConstants.intervalPos, ArmConstants.groundPos, 5.0, GuideConstants.telePos),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            GVFCmd(
                robot.drive,
                SimpleGVFController(intakePath1, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5)
            ),
            WaitCmd(0.25),
            ClawCmds.ClawCloseCmd(robot.claw),
            WaitCmd(0.25),
            InstantCmd({robot.lift.setPos(13.0)}),
            WaitCmd(0.25),
            GVFCmd(
                robot.drive,
                SimpleGVFController(depositPath, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5),
                Pair(
                    DepositSequence(robot.lift, robot.arm, robot.claw, robot.guide, 145.0, LiftConstants.midPos, GuideConstants.depositPos), ProjQuery(
                        Vector(-15.0, 59.0)
                    )
                )
            ),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            WaitCmd(0.25),
            HomeSequence(robot.lift, robot.claw, robot.arm, robot.guide, ArmConstants.intervalPos, ArmConstants.groundPos, 4.0, GuideConstants.telePos),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            GVFCmd(
                robot.drive,
                SimpleGVFController(intakePath2, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5)
            ),
            WaitCmd(0.25),
            ClawCmds.ClawCloseCmd(robot.claw),
            WaitCmd(0.25),
            InstantCmd({robot.lift.setPos(13.0)}),
            WaitCmd(0.25),
            GVFCmd(
                robot.drive,
                SimpleGVFController(depositPath, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5),
                Pair(
                    DepositSequence(robot.lift, robot.arm, robot.claw, robot.guide, 145.0, LiftConstants.midPos, GuideConstants.depositPos), ProjQuery(
                        Vector(-15.0, 59.0)
                    )
                )
            ),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            WaitCmd(0.25),
            HomeSequence(robot.lift, robot.claw, robot.arm, robot.guide, ArmConstants.intervalPos, ArmConstants.groundPos, 3.0, GuideConstants.telePos),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            GVFCmd(
                robot.drive,
                SimpleGVFController(intakePath2, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5)
            ),
            WaitCmd(0.25),
            ClawCmds.ClawCloseCmd(robot.claw),
            WaitCmd(0.25),
            InstantCmd({robot.lift.setPos(13.0)}),
            WaitCmd(0.25),
            GVFCmd(
                robot.drive,
                SimpleGVFController(depositPath, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5),
                Pair(
                    DepositSequence(robot.lift, robot.arm, robot.claw, robot.guide, 145.0, LiftConstants.midPos, GuideConstants.depositPos), ProjQuery(
                        Vector(-15.0, 59.0)
                    )
                )
            ),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            WaitCmd(0.25),
            HomeSequence(robot.lift, robot.claw, robot.arm, robot.guide, ArmConstants.intervalPos, ArmConstants.groundPos, 2.0, GuideConstants.telePos),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            GVFCmd(
                robot.drive,
                SimpleGVFController(intakePath2, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5)
            ),
            WaitCmd(0.25),
            ClawCmds.ClawCloseCmd(robot.claw),
            WaitCmd(0.25),
            InstantCmd({robot.lift.setPos(13.0)}),
            WaitCmd(0.25),
            GVFCmd(
                robot.drive,
                SimpleGVFController(depositPath, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5),
                Pair(
                    DepositSequence(robot.lift, robot.arm, robot.claw, robot.guide, 145.0, LiftConstants.midPos, GuideConstants.depositPos), ProjQuery(
                        Vector(-15.0, 59.0)
                    )
                )
            ),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            WaitCmd(0.25),
            HomeSequence(robot.lift, robot.claw, robot.arm, robot.guide, ArmConstants.intervalPos, ArmConstants.groundPos, 1.0, GuideConstants.telePos),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            GVFCmd(
                robot.drive,
                SimpleGVFController(intakePath2, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5)
            ),
            WaitCmd(0.25),
            ClawCmds.ClawCloseCmd(robot.claw),
            WaitCmd(0.25),
            InstantCmd({robot.lift.setPos(13.0)}),
            WaitCmd(0.25),
            GVFCmd(
                robot.drive,
                SimpleGVFController(depositPath, 0.6, 20.0, 12.0, 0.5, 1.5, 1.5),
                Pair(
                    DepositSequence(robot.lift, robot.arm, robot.claw, robot.guide, 145.0, LiftConstants.midPos, GuideConstants.depositPos), ProjQuery(
                        Vector(-15.0, 59.0)
                    )
                )
            ),
            ClawCmds.ClawOpenCmd(robot.claw, robot.guide, GuideConstants.telePos),
            WaitCmd(0.25),
            HomeSequence(robot.lift, robot.claw, robot.arm, robot.guide, ArmConstants.intervalPos, ArmConstants.homePos, -1.0, GuideConstants.telePos),
            GVFCmd(
                robot.drive,
                SimpleGVFController(middlePath, 0.6, 20.0, 6.0, 0.5, 1.5, 1.5)
            ),
            ChooseCmd(
                GVFCmd(robot.drive,
                    SimpleGVFController(rightPath, 0.6, 20.0, 6.0, 0.5, 1.5, 1.5)),
                ChooseCmd(
                    GVFCmd(robot.drive,
                        SimpleGVFController(middlePath, 0.6, 20.0, 6.0, 0.5, 1.5, 1.5)),
                    GVFCmd(robot.drive, SimpleGVFController(leftPath,0.6, 20.0, 6.0, 0.5, 1.5, 1.5)),
                ) { robot.vision.zone == Enums.Zones.MIDDLE },
            ) { robot.vision.zone == Enums.Zones.RIGHT }
        )
        mainCommand.schedule()
    }

    override fun mInitLoop() {
        Logger.addTelemetryData("zone", robot.vision.zone)
    }

    override fun mStart() {
        robot.vision.stop()
        robot.vision.unregister()
    }

    override fun mLoop() {
        Logger.addTelemetryData("arm pos", robot.arm.motor.pos)
    }
}