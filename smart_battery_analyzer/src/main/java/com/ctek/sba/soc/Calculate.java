package com.ctek.sba.soc;

/**
 * Created by evgeny.akhundzhanov on 27.09.2016.
 */
import java.io.Console;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import android.util.Log;

import greendao.Voltage;

public class Calculate {

  private static final String TAG = "Calculate";

  public interface ILogCallback {
    void LogD (String tag, String mess);
  };

  //SoC version info
  public final static double SOC_ALRORITHM_VERSION = 9.3;

  private final static int BUFFER_SIZE = 5*12*2500;
  private final static int SAMPLE_PERIOD = 5;



  private Queue<SoCData> m_SoCValueQ;     // Queue of stored and calculated data items

  private int         m_bufferSize;       // Size of allocated voltage queue
  private int         m_calcToIndex = 0;  // Pointer in queue to calculate to
  private int         m_calcFromIndex = 0;// Pointer in queue to calculate from
  private int         m_noSamples;        // Number of stored values in queue
  private boolean     m_buffOverflow;     // Have we reached the queuesize?

  private int         m_samplePeriod  = 5; // Sample period, default 5 min
  private double      m_battery_size = 85.0; //Battery size in ampere hours

  // Variables used in calculation (REST mode)
  private boolean     b_restflag;
  private int         m_indxreststart;
  private int         m_tvikth_indx;
  private double      m_d_trest;
  private double      d_i_load;

  // Variables used in calculation (LOAD mode)
  private boolean b_loadflag, b_nextloadflag;

  @SuppressWarnings("unused")
  private int     m_indxloadstart;
  private int     m_dchpts;

  // Variables used in calculation (CHARGE mode)
  private boolean b_chgflag;
  double          d_i_charge;

  // Estimated SoC and calculated voltage
  private double m_dEstSoC;
  private double d_prevlasttruevolt;

  // Colour flag
  private SoCData.StateFlag flag;
  public SoCData.StateFlag getFlag () { return flag; }

  // SoC calculation constants
  final double        d_sfac          = 78;
  final double        d_u0            = 11.55;
  final double        d_chglimit      = 13.20;
  final double        d_loadlimit     = 12.7;


  final double        m_strom_slope = 2;
  final double        m_strom_intercept = 6;
  final int           m_t_viktstart   = 5;
  final int           m_t_viktend     = 10;


  private ILogCallback icbLog;

  public Calculate(int bufferSize, ILogCallback icbLog) {
    this(bufferSize, SAMPLE_PERIOD, icbLog);
  }

  public Calculate(int bufferSize, int samplePer, ILogCallback icbLog) {
    this.icbLog = icbLog;
    // Initialize buffer parameters
    m_samplePeriod      = samplePer;
    m_bufferSize        = bufferSize;
    m_noSamples         = 0;
    m_buffOverflow      = false;

    // Initialize estimated SoC and calculated voltage
    m_dEstSoC = 80.0;
    d_prevlasttruevolt = 12.7;

    // Set index offset for interpolation in REST mode
    m_tvikth_indx = 60 * (m_t_viktstart / m_samplePeriod); // ((60 * m_t_viktstart + m_samplePeriod / 2) / m_samplePeriod);

    // Initial state is REST mode with index set 10 hours back (m_t_viktend)
    b_restflag = true;
    m_indxreststart = -((60 * m_t_viktend + m_samplePeriod / 2) / m_samplePeriod);

    b_loadflag      = false;
    b_nextloadflag  = false;
    m_indxloadstart = 0;
    m_dchpts        = 0;
    b_chgflag       = false;


    // Initialize all flags to off
    flag = SoCData.StateFlag.FLAG_NONE;

    // Allocate queue
    m_SoCValueQ = new ArrayDeque<SoCData>(bufferSize);
  }


  public void addSamples(List<Voltage> voltages) {
    List<Double> voltList = new ArrayList<>();
    for(Voltage voltage : voltages) {
      voltList.add(voltage.getValue());
    }
    addSamples(voltList, null);
  }

  /// <summary>
  /// Add samples to calulation queue
  /// </summary>
  /// <param name="voltList"></param>
  public void addSamples(List<Double> voltList, List<Double> temperatureList) {
    SoCData d;

    // Check that at least one value is stored in queue
    if (m_SoCValueQ.size() > 0) {
      m_calcFromIndex = m_calcToIndex + 1;
    }

    // Loop and add values
    for (int i = 0; i < voltList.size(); i++) {
      // Queuesize reached
      if (m_SoCValueQ.size() >= m_bufferSize)
      {
        // Remove at end of queue
        // m_SoCValueQ.Dequeue();	// Removes and returns the object at the beginning of the Queue<T>.
        m_SoCValueQ.remove();		// Retrieves and removes the head of the queue represented by this deque.
        m_buffOverflow = true;
        // Move "from" pointer backwards
        m_calcFromIndex--;
      }
      // Create new instance of data
      d = new SoCData();
      d.d_voltage = voltList.get(i);
      d.d_temperature = temperatureList!=null ? temperatureList.get(i) : 0;

      // m_SoCValueQ.Enqueue(d);		// Adds an object to the end of the Queue.
      m_SoCValueQ.add(d);				// Inserts the specified element at the end of this deque.

      // Saturate queue counter
      if (m_noSamples < m_bufferSize) {
        m_noSamples++;
      }
      // Set "calculate to" pointer
      m_calcToIndex = m_noSamples-1;
    }

    // Move calculation index pointers backwards if queue is filled
    if (m_buffOverflow) {
      m_indxreststart -= voltList.size();
    }
  }

  public SoCData[] getSoCData() {
    return m_SoCValueQ.toArray(new SoCData[0]);
  }
  public List<SoCData> getSoCDataList () {
    return Arrays.asList(getSoCData());
  }

  public void setBatterySize(double batterySize) { m_battery_size = batterySize; }
  public double getBatterySize() { return m_battery_size; }

  public int getNumberOfSamples() { return m_noSamples; }
  public int getCalcFrom() { return m_calcFromIndex; }
  public int getCalcTo() { return m_calcToIndex; }
  public double getEstSoC() { return m_dEstSoC; }

  public void calcSoCFast() {
    SoCData[] arr_ = getSoCData();
    for (int i = m_calcFromIndex; i <= m_calcToIndex; i++) {
      arr_[i].d_estSoC = 100.d;
    }
    return;
  }

  public void calcSoC() {
    SoCData[] arr_ = getSoCData();
    for (int i = m_calcFromIndex; i <= m_calcToIndex; i++) {
      try {
        // Check mode
        if (i > 0)
        {
          b_loadflag = b_nextloadflag;
          b_nextloadflag = false; //Reset nextloadflag

          //Trig start of discharge. Voltage dip of more then 70 mV and last voltage point is less then chglimit.
          if ((arr_[i].d_voltage < d_loadlimit) &&
              (arr_[i - 1].d_voltage < d_chglimit) &&
              ((arr_[i - 1].d_voltage - arr_[i].d_voltage) > 0.07) &&
              (b_loadflag == false))
          {
            if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. Trig start of discharge.");
            m_indxloadstart = i;
            b_loadflag = true;
            m_dchpts = 0;   //counter for measure points in current discharge
            b_chgflag = false;
            b_restflag = false;
          }
          else if (arr_[i].d_voltage > d_chglimit) //Charge mode
          {
            if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. Charge mode.");
            b_chgflag = true;
            b_restflag = false;
          }
          else   //Rest mode
          {
            if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. Rest mode.");
            // Set index when entering REST mode
            if (b_restflag == false)
            {
              b_restflag = true;
              m_indxreststart = i;
            }
            b_chgflag = false;
            b_restflag = true;
          }
        }

        if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. b_loadflag = " + b_loadflag + " b_restflag = " + b_restflag + " b_chgflag = " + b_chgflag);

        //Load under charge limit
        if( (i>=5) && (arr_[i].d_voltage < 12.6)) // d_chglimit
        {
          double m_sl, dudt;
          double xh;
          double t_dch, slopeAdj;

          if (check3Deltas(arr_, i)) // lim.All( x => x )
          {
            m_sl = ARL(arr_, i, 5);
            dudt = 1000. * m_sl / 3600.;

            if ((dudt < -0.000001) && (arr_[i].d_voltage < 12.9) && (arr_[i].m_lpnr != 5)) {
              //calculation of the C-rate (x-hours current)
              xh = (1.25 * Math.exp(-6.25 * arr_[i - 1].d_estSoC / 100) + 0.25) / dudt;
              t_dch = 4.0 * m_samplePeriod;
              slopeAdj = Math.exp(-t_dch / 20.) + 1.; //Correction factor which corrects some of the overestimation of the current during the first minutes of discharge
              d_i_load = m_battery_size / xh / slopeAdj; //Current caclulation of m_dchpts=3

              arr_[i].d_estSoC = arr_[i - 1].d_estSoC + (100 * d_i_load * m_samplePeriod) / (60 * m_battery_size);
              if (arr_[i].d_estSoC < 0) {
                arr_[i].d_estSoC = 0.;
              }

              d_prevlasttruevolt = arr_[i].d_estSoC / d_sfac + d_u0;
              b_loadflag = true;
              b_restflag = false;
              b_chgflag = false;
              arr_[i].m_lpnr = 6;

              //If discharge current is less then battery_size/100, i.e. less then the 100h-current,
              if (d_i_load > (-m_battery_size / 100.)) {
                d_i_load = 0;
                b_loadflag = false;
                b_nextloadflag = false;
                m_indxloadstart = 0;
                b_restflag = true;
                m_dchpts = 0;
                arr_[i].d_estSoC = arr_[i - 1].d_estSoC;
                arr_[i].m_lpnr = 5;
              }
            }
          }
        }
        if ((i >= 5) && (arr_[i].d_voltage < d_chglimit)) // New for 9.2
        {
          double m_sl, dudt;
          double xh;

          if (check3Deltas(arr_, i)) // lim.All( x => x )
          {
            m_sl = ARL(arr_, i, 5);
            dudt = 1000 * m_sl / 3600;
            if( dudt > 0.000001)
            {
              xh = 0.8 / dudt;
              d_i_charge = m_battery_size / xh;
              d_i_load = d_i_charge;
              arr_[i].d_estSoC = arr_[i - 1].d_estSoC + (100. * d_i_load * m_samplePeriod) / 60.0 / m_battery_size;
              if (arr_[i].d_estSoC > 100.)
              {
                arr_[i].d_estSoC = 100.;
              }
              d_prevlasttruevolt = arr_[i].d_estSoC / d_sfac + d_u0;
              b_loadflag = false;
              b_restflag = false;
              b_chgflag = true;
              arr_[i].m_lpnr = 7;
              if (d_i_charge < (m_battery_size / 100.))
              {
                b_chgflag = false;
                m_dchpts = 0;
                b_restflag = true;
                b_loadflag = false;
                arr_[i].d_estSoC = arr_[i - 1].d_estSoC;
                d_i_charge = 0;
                d_i_load = 0;
                arr_[i].m_lpnr = 5;
              }
            }
          }
        }


        if ((b_chgflag == true) && (arr_[i].d_voltage >= d_chglimit)) // if (b_chgflag == true)
        {
          // -------------------------------------------------------------------------------
          // CHARGE mode, increase SoC based on its previous value
          // -------------------------------------------------------------------------------
          if (i >= 1)
          {
            d_i_charge = m_strom_intercept * Math.sqrt(1. - Math.pow(arr_[i - 1].d_estSoC / 100., m_strom_slope));

            if (d_i_charge < 0.0)
            {
              d_i_charge = 0;
            }
            arr_[i].d_estSoC = arr_[i - 1].d_estSoC + (m_samplePeriod / 60.0) * (d_i_charge / m_battery_size) * 100.0;
            d_prevlasttruevolt = arr_[i].d_estSoC / d_sfac + d_u0;
            arr_[i].m_lpnr = 1;
          }
          if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. CHARGE mode. arr_[i].d_estSoC = " + arr_[i].d_estSoC);
        }
        else if (b_restflag == true && (arr_[i].m_lpnr < 6))
        {
          // -------------------------------------------------------------------------------
          // REST mode, decrease SoC depending on time spent in this mode
          // -------------------------------------------------------------------------------

          // Calculate time spent in REST mode
          m_d_trest = ((double)i - (double)m_indxreststart) / ((double)60.0 / (double)m_samplePeriod);

          // REST mode, first 5 hours
          if (m_d_trest < (double)m_t_viktstart)
          {
            arr_[i].d_estSoC = d_sfac * (d_prevlasttruevolt - d_u0);
            arr_[i].m_lpnr = 3;
            if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. REST mode 1. arr_[i].d_estSoC = " + arr_[i].d_estSoC);
          }

          // REST mode, after 5 - 10 hours
          else if (m_d_trest < (double)m_t_viktend)
          {
            double d_weightfactor = 1.0 - (((double)i - ((double)m_indxreststart + (double)m_tvikth_indx)) / ((double)m_tvikth_indx));

            double meanVal = 0.0;
            if (i >= 5)
            {
              for (int r = i - 5; r <= i; r++)
              {
                meanVal += arr_[r].d_voltage;
              }
              meanVal = meanVal / 6.0;
            }
            else
            {
              meanVal = arr_[i].d_voltage;
            }

            arr_[i].m_lpnr = 4;
            arr_[i].d_estSoC = d_sfac * (double)(d_weightfactor * d_prevlasttruevolt + (1.0 - d_weightfactor) * meanVal - d_u0);
            if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. REST mode 2. arr_[i].d_estSoC = " + arr_[i].d_estSoC);
          }

          // REST mode, after 10 hours
          else
          {
            double meanVal = 0.0;
            if (i >= 5)
            {
              for (int r = i - 5; r <= i; r++)
              {
                meanVal += arr_[r].d_voltage;
              }
              meanVal = meanVal / 6.0;
            }
            else
            {
              meanVal = arr_[i].d_voltage;
            }
            d_prevlasttruevolt = meanVal;

            arr_[i].m_lpnr = 5;
            arr_[i].d_estSoC = d_sfac * (d_prevlasttruevolt - d_u0);
            if(icbLog!=null) icbLog.LogD(TAG, "i = " + (i+1) + "/. REST mode 3. arr_[i].d_estSoC = " + arr_[i].d_estSoC);
          }
        }
        if((b_loadflag == true) && (arr_[i].m_lpnr < 6)) //SoC calculation for discharge mode
        {
          // if (i > 0 && i + 1 <= m_noSamples - 1) // CBS-53 Android - Test SoC algorithm change.
          if ((i > 0) && (i <= (m_noSamples - 1)))
          {
            double m_sl;
            //Trig end of discharge when the voltage is changed with more then +50 mV
            if (arr_[i].d_voltage < (arr_[i - 1].d_voltage + 0.05))
            {
              m_dchpts++; //discharge points counter
              b_nextloadflag = true; //Activate discharge for next measurepoint because next trigger condition will probably not be fullfilled.

              if (m_dchpts < 3)
              { //Let the voltagederivitive stabilize for a while and set current to zero for now
                d_i_load = 0;
                arr_[i].d_estSoC = arr_[i - 1].d_estSoC; //Keep SoC
                d_prevlasttruevolt = arr_[i].d_estSoC / d_sfac + d_u0;
              }
              else
              {
                m_sl = ARL(arr_, i, m_dchpts);
                double dudt = 1000. * m_sl / 3600.; //Voltagederivative expressed in mV/h
                double xh;
                double t_dch, slopeAdj;
                if (dudt < -0.000001)
                {
                  double d_i_load1, d_i_load2;
                  //calculation of the C-rate (x-hours current)
                  xh = (1.25 * Math.exp(-6.25 * arr_[i - 1].d_estSoC / 100) + 0.25) / dudt;
                  t_dch = m_dchpts * m_samplePeriod;
                  slopeAdj = Math.exp(-t_dch / 20) + 1; //Correction factor which corrects some of the overestimation of the current during the first minutes of discharge
                  d_i_load = m_battery_size / xh / slopeAdj; //Current caclulation of m_dchpts=3

                  // Algorithm 9.3 "single line" change.
                  // Limit the calculated current to avoid sudden drops in SoC.
                  if(d_i_load < -100) {
                    d_i_load = -100;
                  }

                  d_i_load1 = 0.75 * d_i_load;          //Current caclulation of m_dchpts=2
                  d_i_load2 = 0.5 * d_i_load;           //Current caclulation of m_dchpts=1
                  //If discharge current is less then battery_size/100, i.e. less then the 100h-current,
                  //the end of the discharge state is trigged (discharge current has always a negative sign.
                  if (d_i_load > -m_battery_size / 100)
                  {
                    d_i_load = 0;
                    b_loadflag = false;
                    b_nextloadflag = false;
                    m_indxloadstart = 0;

                  }
                  //SoC calculation
                  if (m_dchpts == 3)
                  {
                    arr_[i - 2].d_estSoC = arr_[i - 3].d_estSoC + 100 * d_i_load2 * ((double)m_samplePeriod) / 60 / m_battery_size;
                    arr_[i - 1].d_estSoC = arr_[i - 2].d_estSoC + 100 * d_i_load1 * ((double)m_samplePeriod) / 60 / m_battery_size;
                  }
                  arr_[i].d_estSoC = arr_[i - 1].d_estSoC + 100 * d_i_load * ((double)m_samplePeriod) / 60 / m_battery_size;
                  d_prevlasttruevolt = arr_[i].d_estSoC / d_sfac + d_u0;
                  if (d_i_load > (-m_battery_size / 100))
                  {
                    m_dchpts = 0;
                  }

                }
                //If voltagederivative is positive, end of discharge state is trigged
                else
                {
                  b_loadflag = false;
                  b_nextloadflag = false;
                  m_indxloadstart = 0;
                  m_dchpts = 0;
                  arr_[i].d_estSoC = arr_[i - 1].d_estSoC; //Keep SoC
                }
              }
            }
            else //End of discharge
            {
              b_loadflag = false;
              b_nextloadflag = false;
              m_indxloadstart = 0;
              m_dchpts = 0;
              arr_[i].d_estSoC = arr_[i - 1].d_estSoC;
            }
          }

        }

        // Saturation 0 - 100 %
        saturation (arr_[i]);
        // Console.WriteLine(arr_[i].d_voltage + ";" + arr_[i].d_temperature + ";" + m_dEstSoC);
        if(icbLog!=null) icbLog.LogD(TAG, "" + (i+1) + "/. " + arr_[i].d_voltage + " - " + arr_[i].d_estSoC + " / " + m_dEstSoC);
        m_dEstSoC = arr_[i].d_estSoC;

        // -------------------------------------------------------------------------------
        // Set some debug variables
        // -------------------------------------------------------------------------------
        saveDebugParams(arr_[i]);

      }
      catch(Exception xpt) {
        xpt.printStackTrace();
        if(icbLog!=null) icbLog.LogD(TAG, "calcSoC failed on item " + (i + 1) + ". Error: " + xpt.getMessage());

        arr_[i].d_estSoC = (i > 0) ? arr_[i - 1].d_estSoC : m_dEstSoC;
      }
    } // i

    // -------------------------------------------------------------------------------
    // DONE, set flags according to SoC
    // -------------------------------------------------------------------------------
    flag = SoCData.getFlag4Value(m_dEstSoC);
    return;
  }

  // lim.All( x => x )
  private boolean check3Deltas (SoCData[] arr_, int i) {
    final double  dEpsilon   = 0.11;
    boolean lim_1 = Math.abs(arr_[i - 3].d_voltage - arr_[i - 2].d_voltage) < dEpsilon;
    boolean lim_2 = Math.abs(arr_[i - 2].d_voltage - arr_[i - 1].d_voltage) < dEpsilon;
    boolean lim_3 = Math.abs(arr_[i - 1].d_voltage - arr_[i - 0].d_voltage) < dEpsilon;
    return (lim_1 && lim_2 && lim_3);
  }


  private static final double MAX_SOC = 100.f;
  private static final double MIN_SOC =   0.f;

  private void saturation (SoCData soc_) {
    if (soc_.d_estSoC > MAX_SOC)  {
      soc_.d_estSoC = MAX_SOC;
    }
    else if (soc_.d_estSoC < MIN_SOC)  {
      soc_.d_estSoC = MIN_SOC;
    }
    return;
  }

  private void saveDebugParams (SoCData soc_) {
    soc_.i_loadFlag     = b_loadflag ? 1 : 0;
    soc_.i_restFlag     = b_restflag ? 1 : 0;
    soc_.i_chargeFlag   = b_chgflag ? 1 : 0;
    soc_.i_nextloadflag = b_nextloadflag ? 1 : 0;
    soc_.d_tRest        = m_d_trest;
    soc_.d_prevLastTrV  = d_prevlasttruevolt;
    soc_.d_iLoad        = d_i_load;
    return;
  }

  private double ARL(SoCData[] arr_, int k, int dchpts) throws Exception {

    double[] y1;
    if (dchpts == 3) {
      y1 = new double[] { arr_[k - 1].d_voltage, arr_[k].d_voltage };
    }
    else if (dchpts == 4) {
      y1 = new double[] { arr_[k - 2].d_voltage, arr_[k-1].d_voltage, arr_[k].d_voltage };
    }
    else {
      y1 = new double[] { arr_[k - 3].d_voltage, arr_[k-2].d_voltage, arr_[k-1].d_voltage, arr_[k].d_voltage };
    }

    int lVolt = y1.length;


    // Create time array
    double[] cm = new double[2 * lVolt];
    for (int i = 0; i < 2 * lVolt; i++) {
      if (i < lVolt) cm[i] = 1;
      else cm[i] = ((double) (i-lVolt)) * ((double) m_samplePeriod)/60.0;
    }

    Matrix AA  = denseOfColumnMajor(lVolt, 2, cm);
    Matrix yn = denseOfColumnMajor(lVolt, 1, y1);

    // A typical linear algebra problem is the regression normal equation XTy = XTXpXTy = XTXp
    // which we would like to solve for pp.
    // By matrix inversion we get
    // pp = (XT*X)^−1 * (XT * y)
    // This can directly be translated to the following code:
    // (X.Transpose() * X).Inverse() * (X.Transpose() * y)

    // Below:
    // X  - AA
    // XT - AT
    // AtA = AT * AA
    // AM1 = (AT * AA)^−1
    // BB = AT * Y
    // CC = the result

    Matrix AT = AA.transpose();
    Matrix AtA = AT.times(AA);
    Matrix AM1  = AtA.inverse();

    Matrix BB = AT.times(yn);
    Matrix CC = AM1.times(BB);
    return CC.get(1, 0);
  }

  private static Matrix denseOfColumnMajor (int M, int N, double[] values) throws Exception {
    if(M*N == values.length) {
      double[][] data = new double[M][N];
      for(int row = 0; row < M; ++row) {
        for(int col = 0; col < N; ++col) {
          // data[row][col] = values[row * N + col]; // EA 12-Apr-2017. Here the bug was fixed.
          data[row][col] = values[col * M + row];
        }
      }
      return new Matrix(data);
    }
    throw new Exception("Invalid call to denseOfColumnMajor.");
  }


  public SoCData.StateFlag testVoltageList (List<Double> m_voltageList) {
    SoCData.StateFlag m_OldFlag = SoCData.StateFlag.FLAG_GREEN;
    SoCData.StateFlag m_NewFlag = SoCData.StateFlag.FLAG_NONE;

    try {
      addSamples(m_voltageList, null);
      calcSoC();

      m_NewFlag = getFlag();

      // Notifications on transitions
      if ((m_NewFlag != SoCData.StateFlag.FLAG_GREEN) && (m_NewFlag != m_OldFlag)) {
        // Notify
      }
      return m_OldFlag = m_NewFlag;
    }
    catch (Exception xpt) {
      xpt.printStackTrace();
      m_NewFlag = SoCData.StateFlag.FLAG_NONE;
    }
    Log.d(TAG, "New Flag = " + m_NewFlag.toString());
    return m_NewFlag;
  }

} // EOClass
