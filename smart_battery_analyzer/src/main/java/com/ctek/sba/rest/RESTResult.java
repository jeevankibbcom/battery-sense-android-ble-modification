package com.ctek.sba.rest;

/**
 * Created by evgeny.akhundzhanov on 28.08.2017.
 */

public class RESTResult {

  protected int		result;
  protected String	error;

  public RESTResult () {
    result = REST.RESULT_UNDEFINED;
    error = "";
  }

  public RESTResult (int result, String error) {
    this.result = result;
    this.error = error;
  }

  public void setResult (int result, String error) {
    this.result = result;
    this.error	= error;
  }

  public void setSuccess () {
    this.result = REST.RESULT_SUCCESS;
    error = "";
  }

  public void setSuccess (String message) {
    this.result = REST.RESULT_SUCCESS;
    error = message;
  }

  public boolean isSuccess () {
    return result == REST.RESULT_SUCCESS;
  }

  public String getError () { return error; }
  public int getResult () { return result; }

} // EOClass RESTResult
