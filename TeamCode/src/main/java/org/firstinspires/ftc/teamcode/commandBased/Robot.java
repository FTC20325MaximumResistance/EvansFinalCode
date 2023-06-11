package org.firstinspires.ftc.teamcode.commandBased;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.arcrobotics.ftclib.command.CommandScheduler;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.commandBased.commands.MoveArmIncrementally;
import org.firstinspires.ftc.teamcode.commandBased.commands.MoveArmToAngle;
import org.firstinspires.ftc.teamcode.commandBased.commands.MoveElevator;
import org.firstinspires.ftc.teamcode.commandBased.commands.drive.PointCentric;
import org.firstinspires.ftc.teamcode.commandBased.commands.drive.FieldCentric;
import org.firstinspires.ftc.teamcode.commandBased.commands.drive.RobotCentric;
import org.firstinspires.ftc.teamcode.commandBased.subsystems.ArmSubsystem;
import org.firstinspires.ftc.teamcode.commandBased.subsystems.DrivetrainSubsystem;
import org.firstinspires.ftc.teamcode.commandBased.subsystems.ElevatorSubsystem;
import org.firstinspires.ftc.teamcode.rr.util.DashboardUtil;


import ftc.rogue.blacksmith.BlackOp;
import ftc.rogue.blacksmith.Scheduler;
import ftc.rogue.blacksmith.listeners.ReforgedGamepad;

@Config
@TeleOp(name="Command Based", group="Linear Opmode")
public class Robot extends BlackOp {

    //declare subsystem variables
    public static DrivetrainSubsystem drivetrainSS;
    public static ElevatorSubsystem elevatorSS;
    public static ArmSubsystem armSS;

    @Override
    public void go() {

        //cancel all previous commands
        CommandScheduler.getInstance().reset();

        //create subsystem objects
        drivetrainSS = new DrivetrainSubsystem(hardwareMap);
        elevatorSS = new ElevatorSubsystem(hardwareMap);
        armSS = new ArmSubsystem(hardwareMap);

        //create gamepads
        ReforgedGamepad driver = new ReforgedGamepad(gamepad1);
        ReforgedGamepad operator = new ReforgedGamepad(gamepad2);

        //create drivetrain mode commands
        FieldCentric fieldCentric = new FieldCentric(
                drivetrainSS,
                driver.left_stick_x::get,
                () -> -driver.left_stick_y.get(),
                driver.right_stick_x::get
        );
        RobotCentric robotCentric = new RobotCentric(
                drivetrainSS,
                driver.left_stick_x::get,
                () -> -driver.left_stick_y.get(),
                driver.right_stick_x::get
        );
        PointCentric pointCentric = new PointCentric(
                drivetrainSS,
                driver.left_stick_x::get,
                () -> -driver.left_stick_y.get(),
                Constants.TARGET,
                Constants.ANGLE_OFFSET
        );

        //create elevator commands
        MoveElevator eleLow = new MoveElevator(elevatorSS, Constants.ELE_LOW);
        MoveElevator eleMidLow = new MoveElevator(elevatorSS, Constants.ELE_MID_LOW);
        MoveElevator eleMidHigh = new MoveElevator(elevatorSS, Constants.ELE_MID_HIGH);
        MoveElevator eleHigh = new MoveElevator(elevatorSS, Constants.ELE_HIGH);

        //create arm commands
        MoveArmToAngle armBackward = new MoveArmToAngle(armSS, Constants.ARM_ANGLE_BACK);
        MoveArmToAngle armIdle = new MoveArmToAngle(armSS, Constants.ARM_ANGLE_IDLE);
        MoveArmToAngle armForward = new MoveArmToAngle(armSS, Constants.ARM_ANGLE_FRONT);

        //start robot in field-centric mode
        robotCentric.schedule();

        waitForStart();



        Scheduler.launchOnStart(this, () -> {

            // Declare telemetry packet for dashboard field drawing
            TelemetryPacket packet = new TelemetryPacket();
            Canvas fieldOverlay = packet.fieldOverlay();

            //activate scheduler
            CommandScheduler.getInstance().run();

            //drivetrain speed controls
            driver.left_bumper.onRise(() -> drivetrainSS.setSpeedMultipliers(0.5, 0.5, 0.5))
                              .onFall(() -> drivetrainSS.setSpeedMultipliers(1, 1, 1));

            //drivetrain mode controls
            driver.a.onRise(() -> {
                pointCentric.cancel();
                fieldCentric.cancel();
                robotCentric.schedule();
            });
            driver.b.onRise(() -> {
                pointCentric.cancel();
                robotCentric.cancel();
                fieldCentric.schedule();
            });
            driver.x.onRise(() -> {
                fieldCentric.cancel();
                robotCentric.cancel();
                pointCentric.schedule();
            });

            driver.y.onRise(() -> {
                drivetrainSS.resetGyro();
            });

            //elevator controls
            driver.dpad_down.onRise(eleLow::schedule);
            driver.dpad_left.onRise(eleMidLow::schedule);
            driver.dpad_right.onRise(eleMidHigh::schedule);
            driver.dpad_up.onRise(eleHigh::schedule);

            //arm controls
            driver.b.onRise(armForward::schedule);
            driver.x.onRise(armBackward::schedule);
            driver.a.onRise(armIdle::schedule);

            // Draw the target on the field
            fieldOverlay.setStroke("#dd2c00");
            fieldOverlay.strokeCircle(Constants.TARGET.getX(), Constants.TARGET.getY(), 3);

            // Draw bot on canvas
            fieldOverlay.setStroke("#3F51B5");
            DashboardUtil.drawRobot(fieldOverlay, drivetrainSS.getPose());

            // Send telemetry packet off to dashboard
            FtcDashboard.getInstance().sendTelemetryPacket(packet);

            mTelemetry().addData("arm target", armSS.getArmTarget());
            mTelemetry().addData("arm pos", armSS.getArmPos());
            mTelemetry().addData("arm power", armSS.getArmPower());
            mTelemetry().addData("arm angle", armSS.getArmAngle());
            mTelemetry().addData("KP", armSS.getCoeffs()[0]);
            mTelemetry().addData("KI", armSS.getCoeffs()[1]);
            mTelemetry().addData("KD", armSS.getCoeffs()[2]);
            mTelemetry().addData("KV", armSS.getCoeffs()[3]);
            mTelemetry().addData("KA", armSS.getCoeffs()[4]);
            mTelemetry().addData("KS", armSS.getCoeffs()[5]);
            mTelemetry().addData("KF", armSS.getCoeffs()[6]);
            mTelemetry().update();
        });
    }
}