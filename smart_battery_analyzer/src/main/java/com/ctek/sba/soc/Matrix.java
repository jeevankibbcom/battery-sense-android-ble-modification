package com.ctek.sba.soc;

/**
 * Created by evgeny.akhundzhanov on 27.09.2016.
 * Standard Matrix class
 * Code from http://introcs.cs.princeton.edu/java/95linear/Matrix.java.html
 */


/******************************************************************************
 *  Compilation:  javac Matrix.java
 *  Execution:    java Matrix
 *
 *  A bare-bones immutable data type for M-by-N matrices.
 *
 ******************************************************************************/
final public class Matrix {

  private final int M;             // number of rows
  private final int N;             // number of columns
  private final double[][] data;   // M-by-N array

  public double get (int i, int j) { return data[i][j]; }
  public int getRows () { return M; }
  public int getCols () { return N; }

  // create M-by-N matrix of 0's
  public Matrix(int M, int N) {
    this.M = M;
    this.N = N;
    data = new double[M][N];
  }

  // create matrix based on 2d array
  public Matrix(double[][] data) {
    M = data.length;
    N = data[0].length;
    this.data = new double[M][N];
    for (int i = 0; i < M; i++)
      for (int j = 0; j < N; j++)
        this.data[i][j] = data[i][j];
  }

  // copy constructor
  private Matrix(Matrix A) { this(A.data); }

  // create and return a random M-by-N matrix with values between 0 and 1
  public static Matrix random(int M, int N) {
    Matrix A = new Matrix(M, N);
    for (int i = 0; i < M; i++)
      for (int j = 0; j < N; j++)
        A.data[i][j] = Math.random();
    return A;
  }

  // create and return the N-by-N identity matrix
  public static Matrix identity(int N) {
    Matrix I = new Matrix(N, N);
    for (int i = 0; i < N; i++)
      I.data[i][i] = 1;
    return I;
  }

  // swap rows i and j
  private void swap(int i, int j) {
    double[] temp = data[i];
    data[i] = data[j];
    data[j] = temp;
  }

  // create and return the transpose of the invoking matrix
  public Matrix transpose() {
    Matrix A = new Matrix(N, M);
    for (int i = 0; i < M; i++)
      for (int j = 0; j < N; j++)
        A.data[j][i] = this.data[i][j];
    return A;
  }

  // return C = A + B
  public Matrix plus(Matrix B) {
    Matrix A = this;
    if (B.M != A.M || B.N != A.N) throw new RuntimeException("Illegal matrix dimensions.");
    Matrix C = new Matrix(M, N);
    for (int i = 0; i < M; i++)
      for (int j = 0; j < N; j++)
        C.data[i][j] = A.data[i][j] + B.data[i][j];
    return C;
  }


  // return C = A - B
  public Matrix minus(Matrix B) {
    Matrix A = this;
    if (B.M != A.M || B.N != A.N) throw new RuntimeException("Illegal matrix dimensions. Matrix A is " + A.M + "*" + A.N + ". Matrix B is " + B.M + "*" + B.N + ".");
    Matrix C = new Matrix(M, N);
    for (int i = 0; i < M; i++)
      for (int j = 0; j < N; j++)
        C.data[i][j] = A.data[i][j] - B.data[i][j];
    return C;
  }

  // does A = B exactly?
  public boolean eq(Matrix B) {
    Matrix A = this;
    if (B.M != A.M || B.N != A.N) throw new RuntimeException("Illegal matrix dimensions. Matrix A is " + A.M + "*" + A.N + ". Matrix B is " + B.M + "*" + B.N + ".");
    for (int i = 0; i < M; i++)
      for (int j = 0; j < N; j++)
        if (A.data[i][j] != B.data[i][j]) return false;
    return true;
  }

  // return C = A * B
  public Matrix times(Matrix B) {
    Matrix A = this;
    if (A.N != B.M) {
      throw new RuntimeException("Illegal matrix dimensions. A.N = " + A.N + ". B.M = " + B.M);
    }
    Matrix C = new Matrix(A.M, B.N);
    for (int ii = 0; ii < C.M; ii++) {
      for (int jj = 0; jj < C.N; jj++) {
        C.data[ii][jj] = 0.;
        for (int kk = 0; kk < A.N; kk++) {
          double aIK = A.data[ii][kk];
          double bKJ = B.data[kk][jj];
          C.data[ii][jj] += (aIK * bKJ);
        }
      }
    }
    return C;
  }

  // return x = A^-1 b, assuming A is square and has full rank
  public Matrix solve(Matrix rhs) {
    if (M != N || rhs.M != N || rhs.N != 1)
      throw new RuntimeException("Illegal matrix dimensions.");

    // create copies of the data
    Matrix A = new Matrix(this);
    Matrix b = new Matrix(rhs);

    // Gaussian elimination with partial pivoting
    for (int i = 0; i < N; i++) {

      // find pivot row and swap
      int max = i;
      for (int j = i + 1; j < N; j++)
        if (Math.abs(A.data[j][i]) > Math.abs(A.data[max][i]))
          max = j;
      A.swap(i, max);
      b.swap(i, max);

      // singular
      if (A.data[i][i] == 0.0) throw new RuntimeException("Matrix is singular.");

      // pivot within b
      for (int j = i + 1; j < N; j++)
        b.data[j][0] -= b.data[i][0] * A.data[j][i] / A.data[i][i];

      // pivot within A
      for (int j = i + 1; j < N; j++) {
        double m = A.data[j][i] / A.data[i][i];
        for (int k = i+1; k < N; k++) {
          A.data[j][k] -= A.data[i][k] * m;
        }
        A.data[j][i] = 0.0;
      }
    }

    // back substitution
    Matrix x = new Matrix(N, 1);
    for (int j = N - 1; j >= 0; j--) {
      double t = 0.0;
      for (int k = j + 1; k < N; k++)
        t += A.data[j][k] * x.data[k][0];
      x.data[j][0] = (b.data[j][0] - t) / A.data[j][j];
    }
    return x;

  }


  private static double[][] invert(double a[][])
  {
    int n = a.length;
    double x[][] = new double[n][n];
    double b[][] = new double[n][n];
    int index[] = new int[n];
    for (int i=0; i<n; ++i)
      b[i][i] = 1;

    // Transform the matrix into an upper triangle
    gaussian(a, index);

    // Update the matrix b[i][j] with the ratios stored
    for (int i=0; i<n-1; ++i)
      for (int j=i+1; j<n; ++j)
        for (int k=0; k<n; ++k)
          b[index[j]][k]
              -= a[index[j]][i]*b[index[i]][k];

    // Perform backward substitutions
    for (int i=0; i<n; ++i)
    {
      x[n-1][i] = b[index[n-1]][i]/a[index[n-1]][n-1];
      for (int j=n-2; j>=0; --j)
      {
        x[j][i] = b[index[j]][i];
        for (int k=j+1; k<n; ++k)
        {
          x[j][i] -= a[index[j]][k]*x[k][i];
        }
        x[j][i] /= a[index[j]][j];
      }
    }
    return x;
  }

  // Method to carry out the partial-pivoting Gaussian elimination.  Here index[] stores pivoting order.
  private static void gaussian(double a[][], int index[])
  {
    int n = index.length;
    double c[] = new double[n];

    // Initialize the index
    for (int i=0; i<n; ++i)
      index[i] = i;

    // Find the rescaling factors, one from each row
    for (int i=0; i<n; ++i)
    {
      double c1 = 0;
      for (int j=0; j<n; ++j)
      {
        double c0 = Math.abs(a[i][j]);
        if (c0 > c1) c1 = c0;
      }
      c[i] = c1;
    }

    // Search the pivoting element from each column
    int k = 0;
    for (int j=0; j<n-1; ++j)
    {
      double pi1 = 0;
      for (int i=j; i<n; ++i)
      {
        double pi0 = Math.abs(a[index[i]][j]);
        pi0 /= c[index[i]];
        if (pi0 > pi1)
        {
          pi1 = pi0;
          k = i;
        }
      }

      // Interchange rows according to the pivoting order
      int itmp = index[j];
      index[j] = index[k];
      index[k] = itmp;
      for (int i=j+1; i<n; ++i)
      {
        double pj = a[index[i]][j]/a[index[j]][j];

        // Record pivoting ratios below the diagonal
        a[index[i]][j] = pj;

        // Modify other elements accordingly
        for (int l=j+1; l<n; ++l)
          a[index[i]][l] -= pj*a[index[j]][l];
      }
    }
  }

  Matrix inverse () {
    return new Matrix(invert(data));
  }

} // EOClass Matrix
