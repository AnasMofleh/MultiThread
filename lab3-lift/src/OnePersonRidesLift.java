import lift.LiftView;

import java.util.Random;

public class OnePersonRidesLift {

    public static void main(String[] args) throws InterruptedException {
        LiftView lf = new LiftView();
        Monitor m = new Monitor(lf);
        Thread t1 = new Lift(m, lf);
        t1.start();

        for (int i = 0; i < 20 ; i++){
            Thread t2 = new Person(m, m.liftView);
            int rand = new Random().nextInt(2500);
            Thread.sleep(rand);

            t2.start();
        }







        // original kod
       /* LiftView view = new LiftView();

        Passenger passenger = view.createPassenger();

        int from = passenger.getStartFloor();
        int to   = passenger.getDestinationFloor();


        passenger.begin();              // walk in (from left)
        if (from != 0) {
            view.moveLift(0, from);
        }

        passenger.enterLift();          // step inside
        view.moveLift(from, to);
        passenger.exitLift();           // leave lift
        passenger.end();   */             // walk out (to the right)
    }
}
