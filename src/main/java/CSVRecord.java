/**
 * The type Csv record.
 */
public class CSVRecord {

  private double startTime;
  private String requestType;
  private long latency;
  private int responseCode;

  /**
   * Instantiates a new Csv record.
   *
   * @param startTime    the start time
   * @param requestType  the request type
   * @param latency      the latency
   * @param responseCode the response code
   */
  public CSVRecord(double startTime, String requestType, long latency, int responseCode) {
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.responseCode = responseCode;
  }

  /**
   * Gets start time.
   *
   * @return the start time
   */
  public double getStartTime() {
    return startTime;
  }

  /**
   * Gets request type.
   *
   * @return the request type
   */
  public String getRequestType() {
    return requestType;
  }

  /**
   * Gets latency.
   *
   * @return the latency
   */
  public double getLatency() {
    return latency;
  }

  /**
   * Gets response code.
   *
   * @return the response code
   */
  public int getResponseCode() {
    return responseCode;
  }

  @Override
  public String toString() {
    return startTime +
        ", " + requestType +
        ", " + latency +
        ", " + responseCode;
  }
}
