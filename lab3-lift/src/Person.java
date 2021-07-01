import lift.LiftView;
import lift.Passenger;

public class Person extends Thread {
    private Monitor m;
    private LiftView lf;
    private Passenger p;
    private final int from,to;


    public Person(Monitor m, LiftView lf){
        this.m = m;
        this.lf = lf;
        this.p = lf.createPassenger();
        this.from = p.getStartFloor();
        this.to = p.getDestinationFloor();
    }

    @Override
    public void run() {
            try {
                p.begin();

                m.rideLift(p, from, to);

                p.end();


            } catch (InterruptedException e) {
                e.printStackTrace();
            }

    }

}
