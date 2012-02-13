import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.text.TextWindow;
import ij.text.TextPanel;
import java.lang.Math;
import ij.plugin.frame.RoiManager;


public class Transform_Unregistered_Image implements PlugInFilter {
    ImagePlus imp;
    int w,h;
    TextPanel tp_coeff;
    static Boolean r_to_g=false;


    int skip;

    double[] ax=new double[3];
    double[] bx=new double[3];
    double[] ay=new double[3];
    double[] by=new double[3];
    String title;
    String coeff_filename;

    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	return DOES_8G+DOES_16+DOES_32+STACK_REQUIRED+NO_UNDO;
    }
    
    public void run(ImageProcessor ip) {

	w=imp.getWidth();
	h=imp.getHeight();
	int nSlices=imp.getStackSize();
	
	Frame[] FR = WindowManager.getNonImageWindows();
	
	if (FR==null) {
	    IJ.error("No tables are open.");
	    return;
	}
	
	int ii=0;
	String frTitle="";
	boolean coeffWinExists=false;
	do {
	    if (FR[ii]!=null) {
		frTitle = FR[ii].getTitle();
		if (frTitle.toLowerCase().contains("coefficients")) {
			if ((FR[ii] instanceof TextWindow)) {
			    tp_coeff=((TextWindow)FR[ii]).getTextPanel();
			    coeffWinExists=true;
			    coeff_filename=FR[ii].getTitle();
			    //			    IJ.log("coefficients file="+coeff_filename);
			} else {
			    IJ.showMessage("The coefficients file is not opened in the text window. Use 'import text file'.");
			    return;
			}
		}
	    }
	} while (++ii<FR.length);
	
	title=imp.getShortTitle();
	    
	GenericDialog gd = new GenericDialog("Transform_Unregistered_Image", IJ.getInstance());
	gd.addMessage("First, open an image stack of the source channel and a coefficient file.");
	gd.addCheckbox("Reverse mapping (Red to Green)", r_to_g);
	
	gd.showDialog();
	if (gd.wasCanceled()) 
	    return;
	
	r_to_g=gd.getNextBoolean();

	if (r_to_g) 
		title=title+"_RtoG";
	else
	    title=title+"_GtoR";
	
	int imax_coeff=tp_coeff.getLineCount();
	if (imax_coeff!=28) {
	    IJ.showMessage("Number of coefficients does not match 2-D cubic fit.");
	    return;
	}
	String line_coeff=null;
	
	if (r_to_g) 
	    skip = 0;    //Changed!
	else
	    skip = 14;   //Changed!
	
	line_coeff=tp_coeff.getLine(skip);
	double cx=Double.parseDouble(line_coeff);
	for (int i=0;i<3;i++) {
	    line_coeff=tp_coeff.getLine(skip+i+1);
	    ax[i]=Double.parseDouble(line_coeff);
	}
	for (int i=0;i<3;i++) {
	    line_coeff=tp_coeff.getLine(skip+i+4);
	    bx[i]=Double.parseDouble(line_coeff);
	}
	double cy=Double.parseDouble(line_coeff);
	for (int i=0;i<3;i++) {
	    line_coeff=tp_coeff.getLine(skip+i+8);
	    ay[i]=Double.parseDouble(line_coeff);
	}
	for (int i=0;i<3;i++) {
	    line_coeff=tp_coeff.getLine(skip+i+11);
	    by[i]=Double.parseDouble(line_coeff);
	    //		IJ.log("by="+by[i]);
	}
	
	FloatProcessor mip=new FloatProcessor(w,h);
	ImageStack mis=new ImageStack(w,h);
	
	for (int slice=1;slice<=nSlices;slice++) {
	    IJ.showProgress(slice,nSlices);
	    ip=imp.getStack().getProcessor(slice);
	    
	    // Sweep the coordinates of the destination image after affine transformation.
	    for (int y=0;y<=h;y++) {
		for (int x=0;x<=w;x++) {
		    double mx=cx+ax[0]*x+ax[1]*Math.pow(x,2)+ax[2]*Math.pow(x,3)+bx[0]*y+bx[1]*Math.pow(y,2)+bx[2]*Math.pow(y,3);
		    double my=cy+ay[0]*x+ay[1]*Math.pow(x,2)+ay[2]*Math.pow(x,3)+by[0]*y+by[1]*Math.pow(y,2)+by[2]*Math.pow(y,3);
		    double mpx=0;
		    if (mx>=0 && mx<w && my>=0 && my<h) 
			mpx=ip.getBicubicInterpolatedPixel(mx,my,ip);
		    // going to read pixel value at the original location before affine transformation.
		    // To go back to the original is the same as the inverse transformation.
		    mip.putPixelValue(x,y,mpx);
		}
	    }
	    mis.addSlice(title+"_fr"+slice, mip.duplicate());
	}
	ImagePlus mimp=new ImagePlus(title, mis);
	mimp.show();
    }
}

