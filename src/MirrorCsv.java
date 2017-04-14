import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexandre on 23/11/2016.
 */
public class MirrorCsv {
    private static PrintWriter pw;
    private static BufferedReader reader;
    static TRACK_NAME track;
    static List<String>  lines = null;
    public static void main(String[] args) {

        generate_mirror(track.A_SPEEDWAY);

    }

    private static String minusS(String s)
    {
        if(s.charAt(0)=='-')
            return s.substring(1);
        else
            return  ("-"+s);

    }
    public static void generate_mirror(String filename)
    {
        boolean reading_success=true;
        int total_line=0;
        try {
            reader = new BufferedReader(new FileReader(filename+".csv"));
            lines = new ArrayList<String>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                total_line++;
            }
        } catch (IOException e) {
            reading_success=false;
            e.printStackTrace();
        }

        if(reading_success) {
            try {
                pw = new PrintWriter(new File(filename + "_mirror.csv"));
                pw.println("ACCELERATION,BRAKE,STEERING,SPEED,TRACK_POSITION,ANGLE_TO_TRACK_AXIS,TRACK_EDGE_0,TRACK_EDGE_1,TRACK_EDGE_2," +
                        "TRACK_EDGE_3,TRACK_EDGE_4,TRACK_EDGE_5,TRACK_EDGE_6,TRACK_EDGE_7,TRACK_EDGE_8,TRACK_EDGE_9,TRACK_EDGE_10," +
                        "TRACK_EDGE_11,TRACK_EDGE_12,TRACK_EDGE_13,TRACK_EDGE_14,TRACK_EDGE_15,TRACK_EDGE_16,TRACK_EDGE_17,TRACK_EDGE_18");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            for(int i=1; i<total_line;++i)
            {
                String act = lines.get(i);
                String[] acts = act.split(",");

                String s = "";
                int maxLength= (acts.length>25?25:acts.length);
                for(int j=0; j<maxLength; ++j)
                {
                    if(maxLength<25)
                        break;
                    if(j==0)
                        s +=acts[j]; //accelerate
                    else if(j==1||j==3)
                        s +=","+acts[j];//brake, speed
                    else if(j==2||j==4||j==5)
                        s +=","+ minusS(acts[j]);//steering,track position, angle to track axis
                    else if(j>5)
                        s +=","+acts[30-j];//sensor track edge 0...18
                }
                pw.println(s);

            }

        }
    }
}
