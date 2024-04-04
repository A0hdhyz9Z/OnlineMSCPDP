package cpjitsdpexperiment;

import java.io.IOException;
import java.io.Writer;

import moa.options.ClassOption;
import moa.tasks.EvaluatePrequential;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;

public class ExpAIO {

    static MainTask currentTask = new EvaluatePrequential();
    static MainTask currentTask2 = new EvaluatePrequential();
    static Writer writer;

    public ExpAIO() {

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
        String waitingTime = "90";
        int sourcedataset = 0;

        /*** Use only for ORB ***/
        String paramsORB = "100;0.4;10;12;1.5;3";


        //		0
        //		0
        //		20
        //		0.99
        //		90
        //		100;0.4;10;12;1.5;3


        String[] datasetsArray = {"tomcat", "JGroups", "spring-integration",
                "camel", "brackets", "nova", "fabric8",
                "neutron", "npm", "BroadleafCommerce"
        };

        //1757 1813
        //2053 2319
        //1116 1345
        for (int i = 0; i <= 9; i++) {
            for (int k = 0; k <= 9; k++) {
                if (i != k) {
                    for (int j = 1; j <= 30; j++) {
                        dsIdx = k;//target
                        arrId = j;
                        sourcedataset = i;
                        String[] args0 = {String.valueOf(dsIdx), String.valueOf(arrId), ens, theta, waitingTime, String.valueOf(sourcedataset)};
                        savedArgs = args0;

                        /*** OOB ***/
//                        String task = "CpjitsdpAIO3  -l (meta.ggc2.meta.WaitForLabelsOOB -i 15 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/" + datasetsArray[dsIdx] + ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[dsIdx] + "-AIO-OOB-" + arrId + "-source-" + datasetsArray[sourcedataset] + ".csv";
                        String task = "CpjitsdpAIO3  -l (meta.ggc2.meta.WaitForLabelsOOB -i 15 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/Commit/" + datasetsArray[dsIdx] + ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[sourcedataset] + "_" + datasetsArray[dsIdx] + "-" + arrId + ".csv";

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


//
//        /*** OOB ***/
//        String task = "CpjitsdpAIO0  -l (meta.ggc2.meta.WaitForLabelsOOB -i 15 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/" + datasetsArray[dsIdx] + ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[dsIdx] + "-AIO-OOB-" + arrId + ".csv";
////        String task = "CpjitsdpAIO2  -l (meta.ggc2.meta.WaitForLabelsOOB -i 15 -s " + ens + " -t " + theta + " -w " + waitingTime + ")  -s  (ArffFileStream -f (datasets/" + datasetsArray[dsIdx] + ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[dsIdx] + "-AIO-OOB-" + arrId + ".csv";
//
//        /*** ORB ***/
//        String task2 = "CpjitsdpAIO3 -l (spdisc.meta.WFL_OO_ORB_Oza -i 15 -s " + ens + " -t " + theta + " -w " + waitingTime + " -p " + paramsORB + ")  -s  (ArffFileStream -f (datasets/" + datasetsArray[dsIdx] + ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d results/" + datasetsArray[dsIdx] + "-AIO-ORB-(" + paramsORB.replaceAll(";", "-") + ")-" + arrId + ".csv";
//
////        try {
////            System.out.println(task);
////            currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);
////            TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);
////
////            thread.start();
////            thread.join();  // 等待第一个线程结束
////
////            System.out.println(task2);
////            currentTask2 = (MainTask) ClassOption.cliStringToObject(task2, MainTask.class, null);
////            TaskThread thread2 = new TaskThread((moa.tasks.Task) currentTask2);
////
////            thread2.start();  // 在第一个线程结束后启动第二个线程
////        } catch (Exception ex) {
////            ex.printStackTrace();
////        }
//
//        ExecutorService executor = null; // 初始化executor
//        try {
//            // 创建一个包含两个线程的线程池
//            executor = Executors.newFixedThreadPool(2);
//
//            System.out.println(task);
//            currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);
//            TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);
//
//            // 启动第一个线程
//            Future<?> future1 = executor.submit(thread);
//
//            // 等待第一个线程结束
//            future1.get();
//
//            System.out.println(task2);
//            currentTask2 = (MainTask) ClassOption.cliStringToObject(task2, MainTask.class, null);
//            TaskThread thread2 = new TaskThread((moa.tasks.Task) currentTask2);
//
//            // 启动第二个线程
//            Future<?> future2 = executor.submit(thread2);
//
//            // 等待第二个线程结束
//            future2.get();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            if (executor != null) {
//                executor.shutdown(); // 关闭ExecutorService，以释放资源
//            }
//        }

    }

}
