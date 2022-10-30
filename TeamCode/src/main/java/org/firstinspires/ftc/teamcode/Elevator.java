package org.firstinspires.ftc.teamcode;

import static org.firstinspires.ftc.teamcode.ElevatorState.*;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import java.util.Arrays;
import java.util.List;

public class Elevator {

    // Elevator Constants
    final double HOME_POWER = -0.1;

    final int ELEVATOR_MIN    = 0;
    final int ELEVATOR_HOME   = 40;
    final int ELEVATOR_GROUND = 160;
    final int ELEVATOR_LOW    = 460;
    final int ELEVATOR_MID    = 710;
    final int ELEVATOR_HIGH   = 1000;
    final int ELEVATOR_MAX    = 1100;

    final int    DEAD_BAND    = 5;
    final double FAST_LIFT    =  0.5;
    final double SLOW_LIFT    =  0.2;
    final double SLOW_LOWER   =  0.0;
    final double FAST_LOWER   = -0.1;
    final double HOLD_POWER   =  0.05;
    final double IN_POSITION_LIMIT = 10;

    // Wrist & Hand Constants
    final double SAFE_WRIST_OFFSET = 90;

    final double WRIST_HOME_POSITION = 0.6;
    final double HAND_HOME_POSITION = 0.8;

    final double HAND_OPEN  = 0.5; //was 0.47
    final double HAND_READY = 0.6 ;
    final double HAND_CLOSE = 0.85;

    // Angle (A) to/from Position (P) conversion factors Y = Mx + C
    final double LIFT_P2A_M = 0.0992;
    final double LIFT_P2A_C = -37.2;

    final double WRIST_A2P_M = 0.00374;
    final double WRIST_A2P_C = 0.472;

    // class members (devices)
    private DcMotorEx       liftMaster;
    private DcMotorEx       liftSlave;
    private List<DcMotorEx> motors;
    private Servo           wrist;  //smaller values tilt the wrist down
    private Servo           hand;   //smaller values open the hand more
    private ElapsedTime     elevatorStateTimer = new ElapsedTime();

    // class members (objects)
    private LinearOpMode myOpMode = null;

    // elevator state variables
    private ElevatorState elevatorState = IDLE;
    private boolean liftActive          = false;
    private boolean liftInPosition      = false;
    private int     liftPosition        = 0;
    private double  liftAngle           = 0;
    private int     liftTargetPosition  = 0;
    private int     liftLastPosition    = 0;

    double pendingDelay;
    int    pendingLiftPosition;
    ElevatorState pendingState;
    int requestedPosition;

    private double  wristOffset = 0;
    private double  wristPosition = 0;
    private double  wristAngle = 0;

    private double  handPosition = 0;

    // Elevator Constructor.  Call once opmode is running.
    public Elevator(LinearOpMode opMode) {
        // Attach to hardware devices
        myOpMode = opMode;
        liftMaster = myOpMode.hardwareMap.get(DcMotorEx.class, "lift_master");
        liftSlave = myOpMode.hardwareMap.get(DcMotorEx.class, "lefte");
        wrist = myOpMode.hardwareMap.get(Servo.class, "wrist");
        hand = myOpMode.hardwareMap.get(Servo.class, "hand");
        motors = Arrays.asList(liftMaster, liftSlave);

        setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        liftMaster.setDirection(DcMotorSimple.Direction.FORWARD);
        liftSlave.setDirection(DcMotorSimple.Direction.REVERSE);
        setWristPosition(WRIST_HOME_POSITION);
        setHandPosition(HAND_HOME_POSITION);
    }

    // ======  Elevator State Machine
    public ElevatorState runStateMachine(ElevatorState newState) {
        setState(newState);
        switch (elevatorState) {
            case IDLE: {
                setState(HOMING);
                break;
            }

            case HOMING: {
                myOpMode.telemetry.addData("Elevator", "Homing");
                myOpMode.telemetry.update();
                recalibrateHomePosition();
                myOpMode.telemetry.addData("Elevator", "Home Done.");
                myOpMode.telemetry.update();
                setState(HOME_OPEN);
                break;
            }

            case HOME_OPEN: {
               if  (newLiftPosition()) {
                   setHandDelayMove(HAND_CLOSE, 0.3, requestedPosition, MOVING_CLOSED);
               } else if (myOpMode.gamepad2.square) {
                   setHandDelayMove(HAND_CLOSE, 0.3, (liftPosition + 50), HOME_CLOSED);
               }
               break;
            }

            case HOME_CLOSED: {
                if  (newLiftPosition()) {
                    setLiftTargetPosition(requestedPosition);
                    setState(MOVING_CLOSED);
                } else if (myOpMode.gamepad2.circle) {
                    setHandPosition(HAND_OPEN);
                    setState(HOME_OPEN);
                }
                break;
            }

            case GOING_HOME_OPEN: {
                if (liftInPosition) {
                    setHandPosition(HAND_OPEN);
                    setState(HOME_OPEN);
                }
                break;
            }

            case WAITING_TO_MOVE:{
                // wait for timer to elapse then go to new position and state.
                // Get here by calling setWristDelayMove()
                if (elevatorStateTimer.time() >= pendingDelay) {
                    setLiftTargetPosition(pendingLiftPosition);
                    setState(pendingState);
                }
                break;
            }

            case MOVING_OPEN: {
                if (liftInPosition) {
                    setState(IN_POSITION_OPEN);
                }
                break;
            }

            case IN_POSITION_OPEN: {
                 if (myOpMode.gamepad2.left_bumper) {
                     setHandDelayMove(HAND_READY, 0.1, ELEVATOR_HOME, GOING_HOME_OPEN);
                 } else if (myOpMode.gamepad2.square) {
                     setHandPosition(HAND_CLOSE);
                     setState(IN_POSITION_CLOSED);
                 }

                break;
            }

            case MOVING_CLOSED: {
                if (liftInPosition) {
                    setState(IN_POSITION_CLOSED);
                }
                break;
            }

            case IN_POSITION_CLOSED: {
                 if (myOpMode.gamepad2.circle) {
                     setHandPosition(HAND_OPEN);
                     setState(IN_POSITION_OPEN);
                 } else if (newLiftPosition()) {
                     setLiftTargetPosition(requestedPosition);
                     setState(MOVING_CLOSED);
                 } else if (myOpMode.gamepad2.left_bumper) {
                     setLiftTargetPosition(ELEVATOR_HOME);
                     setState(HOME_CLOSED);
                 }
                 break;
            }
        }
        return elevatorState;
    }

    /*
    ----------PILOT/CO-PILOT-------------
    ----------ELEVATOR CONTROLS----------
     */
    public boolean newLiftPosition () {
        int newPosition = 0;
        // Select one of the 4 preset heights
        if (myOpMode.gamepad2.dpad_down) {
            newPosition = ELEVATOR_LOW;
        } else if (myOpMode.gamepad2.dpad_left) {
           newPosition = ELEVATOR_MID;
        } else if (myOpMode.gamepad2.dpad_up) {
            newPosition = ELEVATOR_HIGH;
        } else if (myOpMode.gamepad2.dpad_right) {
            newPosition = ELEVATOR_GROUND;
        }
        if(newPosition != 0) {
            requestedPosition = newPosition;
            return true;
        } else {
            return false;
        }
    }
    /***
     * This is the normal way to run the Elevator state machine.
     * Only use the parameter form if you want to force a state change
     * @return current state.
     */
    public ElevatorState runStateMachine() {
        return runStateMachine(elevatorState);
    }

    public void setState(ElevatorState newState) {
        if (newState != elevatorState) {
            elevatorState = newState;
            elevatorStateTimer.reset();
            SharedStates.elevatorState = newState;
        }
    }

    public String getStateText() {
        return elevatorState.toString();
    }


    private void setHandDelayMove(double handPosition, double delaySec,  int elevatorPosition, ElevatorState nextState) {
        setHandPosition(handPosition);
        pendingDelay = delaySec;
        pendingLiftPosition = elevatorPosition;
        pendingState = nextState;

        setState(WAITING_TO_MOVE);
    }

    /**
     * Perform closed loop control on elevator and wrist
     * @return true if lift is In Position
     */
    public boolean update() {
        liftPosition = liftMaster.getCurrentPosition();

        // Run the elevator motor with 4 different speed zones.  Two up and two down.
        if (liftActive) {
            double error = liftTargetPosition - getLiftPosition();
            if (error > DEAD_BAND * 4) {
                // elevator is way too low
                rampPower(FAST_LIFT);
            } else if (error > DEAD_BAND) {
                // elevator is little too low
                setPower(SLOW_LIFT);
            } else if (error < -DEAD_BAND * 4) {
                // elevator is way too High
                rampPower(FAST_LOWER);
            } else if (error < -DEAD_BAND) {
                // elevator is little too High
                setPower(SLOW_LOWER);
            } else {
                // We are in position, so apply a little hold power unless we are at rest on support
                if (liftTargetPosition <= ELEVATOR_HOME)
                    setPower(0);
                else
                    setPower(HOLD_POWER);

            }

            liftInPosition = (Math.abs(error) <= IN_POSITION_LIMIT);

            // Adjust the angle of the servo.
            // first calculate arm angle and negate it for level wrist
            // Then add desired wrist offset angle and send to servo.
            liftAngle = elevatorEncoderToAngle(liftPosition);
            wristAngle = wristOffset - liftAngle;
            wristPosition = wristAngleToServo(wristAngle);
            setWristPosition(wristPosition);

            // Display key arm data
            myOpMode.telemetry.addData("arm position", liftPosition);
            myOpMode.telemetry.addData("arm angle", liftAngle);
            myOpMode.telemetry.addData("servo position", wristPosition);
        }
        return liftInPosition;
    }

    /***
     * Start the power off sloly when moving a long way
     * @param power
     */
    private void rampPower(double power) {
        power += (power - liftMaster.getPower()) * 0.4;
        setPower(power);
    }

    /***
     * Re-learn the elevator home position.
     * Lower the elevator until it stops and then reset zero position.
     */
    public void recalibrateHomePosition() {
        disableLift();  // Stop any closed loop control
        liftLastPosition = liftMaster.getCurrentPosition();
        setPower(HOME_POWER);
        myOpMode.sleep(250);

        while (!myOpMode.isStopRequested() && (liftMaster.getCurrentPosition() != liftLastPosition)){
            liftLastPosition = liftMaster.getCurrentPosition();
            myOpMode.sleep(100);
        }

        setPower(0);
        setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        myOpMode.sleep(50);
        setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        setLiftTargetPosition(0);
        setHandPosition(HAND_OPEN);
        enableLift();  // Start closed loop control
    }


    // ----- Elevator controls
    public void enableLift() {
        liftActive = true;
    }

    public void disableLift() {
        liftActive = false;
    }

    public void setLiftTargetPosition(int Position){
        liftTargetPosition = Range.clip(Position, ELEVATOR_MIN, ELEVATOR_MAX);
        liftInPosition = false;  // set this now to prevent the state machine from skipping
    }

    public int getLiftTargetPosition(){
        return liftTargetPosition;
    }

    public int getLiftPosition() {
        return liftPosition;
    }

    public void jogElevator(double speed) {
        setLiftTargetPosition(liftTargetPosition + (int)(speed * 6));
    }

    // ----- Wrist controls
    public void setWristOffset(double angle){wristOffset = angle;}

    public double getWristOffset () {return wristOffset;}

    public void setWristPosition(double position) {

        wristPosition = position;
        wrist.setPosition(position);
    }

    public void setHandPosition(double position) {
        handPosition = position;
        hand.setPosition(position);
    }

    // ------ Angle/reading conversions
    public double elevatorEncoderToAngle (int position) {
        return(((position * LIFT_P2A_M) + LIFT_P2A_C));
    }

    public int elevatorAngleToEncoder (double angle) {
        return((int)((angle- LIFT_P2A_C)/ LIFT_P2A_M));
    }

    public double wristAngleToServo (double angle) {
        return(((angle * WRIST_A2P_M) + WRIST_A2P_C));
    }

    // ------- Bulk motor control methods
    public void setPower(double power){
        for (DcMotorEx motor : motors) {
            motor.setPower(power);
        }
    }

    public void setMode(DcMotor.RunMode runMode) {
        for (DcMotorEx motor : motors) {
            motor.setMode(runMode);
        }
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior zeroPowerBehavior) {
        for (DcMotorEx motor : motors) {
            motor.setZeroPowerBehavior(zeroPowerBehavior);
        }
    }

}

