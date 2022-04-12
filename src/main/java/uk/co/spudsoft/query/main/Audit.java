/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main;

/**
 *
 * @author jtalbut
 */
public class Audit {

  private final DataSource dataSource;
  private final int retryBaseMs;
  private final int retryIncrementMs;
  private final int retryLimit;

  public DataSource getDataSource() {
    return dataSource;
  }

  public int getRetryBaseMs() {
    return retryBaseMs;
  }

  public int getRetryIncrementMs() {
    return retryIncrementMs;
  }

  public int getRetryLimit() {
    return retryLimit;
  }

  public static class Builder {

    private DataSource dataSource;
    private int retryBaseMs = 20000;
    private int retryIncrementMs = 2000;
    private int retryLimit = 100;

    private Builder() {
    }

    public Builder dataSource(final DataSource value) {
      this.dataSource = value;
      return this;
    }

    public Builder retryBaseMs(final int value) {
      this.retryBaseMs = value;
      return this;
    }

    public Builder retryIncrementMs(final int value) {
      this.retryIncrementMs = value;
      return this;
    }

    public Builder retryLimit(final int value) {
      this.retryLimit = value;
      return this;
    }

    public Audit build() {
      return new uk.co.spudsoft.query.main.Audit(dataSource, retryBaseMs, retryIncrementMs, retryLimit);
    }
  }

  public static Audit.Builder builder() {
    return new Audit.Builder();
  }

  private Audit(final DataSource dataSource, final int retryBaseMs, final int retryIncrementMs, final int retryLimit) {
    this.dataSource = dataSource;
    this.retryBaseMs = retryBaseMs;
    this.retryIncrementMs = retryIncrementMs;
    this.retryLimit = retryLimit;
  }
  
  
}
