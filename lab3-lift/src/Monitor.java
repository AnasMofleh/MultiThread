import lift.LiftView;
import lift.Passenger;

class Monitor {
    LiftView liftView;
    private int[] waitEntry,waitExit;
    private int here,next,load;
    private boolean goingUp, liftIsMoving;


    Monitor(LiftView lf){
        this.liftView = lf;
        this.waitEntry= new int[7];
        this.waitExit = new int[7];

    }

    synchronized void rideLift(Passenger p, int from, int to) throws InterruptedException {

        waitEntry[from] += 1;
        notifyAll();

      while (liftIsMoving || from != here || load == 4) wait();

        p.enterLift();
        load += 1;
        waitExit[to] += 1;
        waitEntry[from] -= 1;
        notifyAll();



    while (to != here || liftIsMoving) wait();
        p.exitLift();
        load -= 1;
        waitExit[to] -= 1;
        notifyAll();

    }

    private synchronized void stopLiftWithCondition() throws InterruptedException {
        while(waitExit[here] > 0 || (waitEntry[here] > 0 && load < 4)) {
            liftIsMoving =false;
            wait();
        }
    }

    public synchronized int getDiraction(int currentFloor) throws InterruptedException{
        here = currentFloor;

        if (goingUp) {

            if (here == 6) {
                goingUp = false;
                next = here - 1;
            } else next = here + 1;


            notifyAll();


            stopLiftWithCondition();


        } else {
            if (here == 0) {
                goingUp = true;
                next = here + 1;
            } else next = here - 1;
            notifyAll();


           stopLiftWithCondition();

        }

        liftIsMoving = true;
        notifyAll();

        return next;
    }




}
