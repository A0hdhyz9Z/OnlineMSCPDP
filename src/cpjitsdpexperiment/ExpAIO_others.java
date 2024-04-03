package cpjitsdpexperiment;

import moa.options.ClassOption;
import moa.tasks.EvaluatePrequential;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;

import java.io.IOException;
import java.io.Writer;

public class ExpAIO_others {

    static MainTask currentTask = new EvaluatePrequential();
    static MainTask currentTask2 = new EvaluatePrequential();
    static Writer writer;

    public ExpAIO_others() {

    }

    private static String[] savedArgs;

    public static String[] getArgs() {
        return savedArgs;
    }

    public static void main(String[] args) throws IOException, InterruptedException {


//		int dsIdx = new Integer(args[0]);
//		int arrId = new Integer(args[1]);
//		String ens =  args[2];
//		String theta = args[3];
//		String waitingTime= args[4];

        int dsIdx = 0;
        int arrId = 0;
        String ens = "20";
        String theta = "0.99";
        String waitingTime = "20";
        int sourcedataset = 0;

//        // Use only for ORB
//        String paramsORB = "100;0.4;10;12;1.5;3";

//        String[] datasetsArray = {"Apache", "Safe", "Zxing"};                 //ReLink
//        String[] datasetsArray = {"activemq-5.0.0", "derby-10.5.1.1", "groovy-1_6_BETA_1",
//                "hbase-0.94.0", "hive-0.9.0", "jruby-1.1", "wicket-1.3.0-beta2"};           //Z-JIRA
        String[] datasetsArray = {"EQ", "JDT", "Lucene", "Mylyn", "PDE"};      //AEEEM

        for (int i = 0; i <= 4; i++) {
            for (int k = 0; k <= 4; k++) {
                if (i != k) {
                    for (int j = 1; j <= 30; j++) {
                        dsIdx = k;//target
                        arrId = j;
                        sourcedataset = i;
                        savedArgs = new String[]{String.valueOf(dsIdx), String.valueOf(arrId), ens, theta, waitingTime, String.valueOf(sourcedataset)};

                        /*** OOB ***/
                        // full source
//                        String task = "CpjitsdpAIO4  -l (meta.ggc2.meta.WaitForLabelsOOB -i 27 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/ReLink/" + datasetsArray[dsIdx] + ".arff) -c 27) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";
//                        String task = "CpjitsdpAIO4  -l (meta.ggc2.meta.WaitForLabelsOOB -i 66 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/" + datasetsArray[dsIdx] + ".arff) -c 66) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";
//                        String task = "CpjitsdpAIO4  -l (meta.ggc2.meta.WaitForLabelsOOB -i 62 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/" + datasetsArray[dsIdx] + ".arff) -c 62) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";

                        // 0.9source
//                        String task = "CpjitsdpAIO4  -l (meta.ggc2.meta.WaitForLabelsOOB -i 27 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets_process/ReLink/" + datasetsArray[sourcedataset] + "_" + j + ".arff) -c 27) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";
//                        String task = "CpjitsdpAIO4  -l (meta.ggc2.meta.WaitForLabelsOOB -i 66 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets_process/Z-JIRA/" + datasetsArray[sourcedataset] + "_" + j + ".arff) -c 66) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";
                        String task = "CpjitsdpAIO4  -l (meta.ggc2.meta.WaitForLabelsOOB -i 62 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets_process/AEEEM/" + datasetsArray[sourcedataset] + "_" + j + ".arff) -c 62) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";

                        /*** ORB ***/
                        // String task = "CpjitsdpAIO -l (spdisc.meta.WFL_OO_ORB_Oza -i 15 -s "+ens+" -t "+theta+" -w "+waitingTime+" -p "+paramsORB+")  -s  (ArffFileStream -f (datasets/"+datasetsArray[dsIdx]+".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/"+datasetsArray[dsIdx]+"-AIO-ORB-("+paramsORB.replaceAll(";", "-")+")-"+arrId+".csv";

                        try {

                            System.out.println(task);
                            currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);

                            TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);

                            thread.start();
                            thread.join();

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
