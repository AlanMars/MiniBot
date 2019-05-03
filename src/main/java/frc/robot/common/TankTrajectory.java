package frc.robot.common;

import jaci.pathfinder.Pathfinder;
import jaci.pathfinder.PathfinderFRC;
import jaci.pathfinder.Trajectory;
import jaci.pathfinder.followers.EncoderFollower;
import edu.wpi.first.wpilibj.Notifier;
import com.kauailabs.navx.frc.AHRS;

import frc.robot.common.GearBox;

public class TankTrajectory{
    GearBox left_gearbox;
    GearBox right_gearbox;

    AHRS gyro;
    
    double max_velocity = 0.0;
    double period;
    boolean gyro_inverted;

    EncoderFollower left_encoderfollower;
    EncoderFollower right_encoderollower;

    Notifier follower_notifier;

    public TankTrajectory(GearBox left_gearbox,  GearBox right_gearbox, AHRS gyro, double period, boolean gyro_inverted){
        this.left_gearbox = left_gearbox;
        this.right_gearbox = right_gearbox;

        this.gyro = gyro;

        this.period = period;

        this.gyro_inverted = gyro_inverted;
    }

    public void configure(int left_ticks, int right_ticks, int ticks_per_rev, int wheel_diameter, double max_velocity){
        this.left_encoderfollower.configureEncoder(left_ticks, ticks_per_rev, wheel_diameter);
        this.right_encoderollower.configureEncoder(right_ticks, ticks_per_rev, wheel_diameter);

        this.max_velocity = max_velocity;

        this.follower_notifier = new Notifier(this::feed);
    }

    public void defaultPID(){
        if (this.max_velocity != 0.0){
            this.left_encoderfollower.configurePIDVA(1.0, 0.0, 0.0, 1 / this.max_velocity, 0);
            this.right_encoderollower.configurePIDVA(1.0, 0.0, 0.0, 1 / this.max_velocity, 0);
        } else {
            System.out.println("TankTrajectory must be configured before using defauldPID");
        }
    }

    public void setPIDA(double p, double i, double d, double a){
        if (this.max_velocity != 0.0){
            this.left_encoderfollower.configurePIDVA(p, i, d, 1 / this.max_velocity, a);
            this.right_encoderollower.configurePIDVA(p, i, d, 1 / this.max_velocity, a);
        } else {
            System.out.println("TankTrajectory must be configured before using setPIDA");
        }
    }

    public void start(){
        this.follower_notifier.startPeriodic(this.period);
    }

    public void stop(){
        this.follower_notifier.stop();
        this.left_gearbox.front.set(0.0);
        this.right_gearbox.front.set(0.0);
    }

    private void feed(){
        if(this.left_encoderfollower.isFinished() || this.right_encoderollower.isFinished()){
            stop();
        } else {
            double left_speed = this.left_encoderfollower.calculate(this.left_gearbox.getTicks());
            double right_speed = this.right_encoderollower.calculate(this.right_gearbox.getTicks());

            double heading;

            if(this.gyro_inverted){
                heading = -this.gyro.getAngle();
            } else {
                heading = this.gyro.getAngle();
            }

            double desired_heading = Pathfinder.r2d(this.left_encoderfollower.getHeading());
            double heading_difference = Pathfinder.boundHalfDegrees(desired_heading - heading);

            double turn =  0.8 * (-1.0/80.0) * heading_difference;

            this.left_gearbox.front.set(left_speed + turn);
            this.right_gearbox.front.set(right_speed - turn);
        }
    }

}