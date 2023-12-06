package org.firstinspires.ftc.teamcode.Autonomous;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.RobotClass;

@Autonomous
public class RedBackstage extends LinearOpMode {
    //Instantiate robot class
    RobotClass teamBot = new RobotClass(this);

    private Object posOfTag;
    public void runOpMode() throws InterruptedException {
        //initialize robot

        teamBot.init(hardwareMap);
        posOfTag = teamBot.findTeamProp(502);

        waitForStart();

            //Strafe to the Wall
        teamBot.strafing(RobotClass.Direction.RIGHT, 0.6, 3000);

    }
}