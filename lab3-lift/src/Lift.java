import lift.LiftView;

public class Lift extends Thread {
    private Monitor m;
    private LiftView lf;
    private int here;

    Lift(Monitor m, LiftView lf){
        this.m = m;
        this.lf = lf;
        this.here = 0;

    }


    @Override
    public void run() {
        while (true) {
            try {

                int to = m.getDiraction(here);

                lf.moveLift(here, to);

                here = to;

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
