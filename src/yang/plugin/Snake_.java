package yang.plugin;

import java.awt.Rectangle;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
		    Initialization by a rectangle, oval or straight line
			Deformation of the snake towards the nearest edges along the
			normals, (edges defined by the low and high threshold values)
			Regularization of the shape by constraing the points to form a
			smooth curve (depending on the values max reg and min reg)
		 	If a closed region, description of the perimeter by Fourier
			descriptors (number of descriptors can be chosen)
		 	Computing of the curvatures values along the perimeter, and display
			in a image.
 *
 */

public class Snake_ implements PlugInFilter{

	ImagePlus imp;

	@Override
	public void run(ImageProcessor ip) {
		int x, y, max, min, p, i, largeur, hauteur, NbPoints;
		int size = 5;
		int ite = 1;
		int sb = 200, sh = 200;
		int four = 10;
		double force = 10.0, regmin = 1.0, regmax = 2.0, cou, cour, maxC, scale;
		
		largeur = ip.getWidth();
		hauteur = ip.getHeight();
		ImageProcessor res = new ByteProcessor(largeur, hauteur);
		res.insert(ip, 0, 0);
		
		Roi r = imp.getRoi();
		Snake snake = new Snake();
		snake.Init(r);
		
		GenericDialog gd = new GenericDialog("Snake", IJ.getInstance());
		gd.addNumericField("Low treshold:", sb, 1);
		gd.addNumericField("High treshold:", sh, 1);
		gd.addNumericField("Min reg:", regmin, 1);
		gd.addNumericField("Max reg:", regmax, 1);
		gd.addNumericField("Number of iterations:", ite, 1);
		if(snake.closed()) gd.addNumericField("Fourier descriptors:", four, 10);
		
		gd.showDialog();
		sb = (int) gd.getNextNumber();
		sh = (int) gd.getNextNumber();
		regmin = (double) gd.getNextNumber();
		regmax = (double) gd.getNextNumber();
		ite = (int) gd.getNextNumber();
		if(snake.closed()) four = (int) gd.getNextNumber();
		
		IJ.write("Calculating snake...");
		for(i=0; i<ite; i++) {
			 IJ.showProgress(0.8*((double)(i) / (double)(ite)));
			 snake.principal(res, force, sb, sh, regmin, regmax);
		}
		IJ.showProgress(0.8);
		if(snake.closed()){
			IJ.write("Segmentation...");
			snake.segmentation(ip);
			if(four>0) snake.drawFourier(ip, four);
			else snake.DrawSnake(ip);
		} else 
			snake.DrawSnake(ip);
		
		new ImagePlus("original",res).show();
		
		IJ.write("Curvature...");	
		maxC = 0.1;
		NbPoints = snake.getNbPoints();
		IJ.showProgress(0.9);
		largeur = NbPoints;
		hauteur = 200;
		ImageProcessor co = new ColorProcessor(largeur, hauteur);
		co.setColor(java.awt.Color.white); co.fill();
		co.setColor(java.awt.Color.black); 
		
		scale = 100.0 / maxC;
		co.setColor(java.awt.Color.black); 
		scale = 100.0 / maxC;
		co.moveTo(0,10);
		co.drawString(""+maxC);
		co.moveTo(0,100);
		co.drawString(""+0.0);
		co.moveTo(0,100);
		co.lineTo(largeur-1,100);
		
		int id, il;
		if(snake.closed()) {id=1;il=NbPoints;co.moveTo(0,100);}
		else {id=10;il=NbPoints-9;co.moveTo(10,100);}
		for(i=id;i<=il;i++){
			cou= snake.courbure(i);
			co.setColor(java.awt.Color.black);
			if(cou < 0.001) co.setColor(java.awt.Color.red);
			else if(cou > 0.001) co.setColor(java.awt.Color.blue);
			co.lineTo(i, (int)(100-scale*cou));
		}
		IJ.showProgress(1.0);
		new ImagePlus("curvature",co).show();

		IJ.wait(5000);
		GenericDialog gdk = new GenericDialog("Curvature",IJ.getInstance());
		gdk.addCheckbox("Output curvature values",false);
		gdk.showDialog();
		boolean kval = gdk.getNextBoolean();
		if(kval) IJ.write("Curvature:");
		if(snake.closed()) {id=1;il=NbPoints;co.moveTo(0,100);}
		else {id=10;il=NbPoints-9;co.moveTo(10,100);}
		if(kval) {
			for(i=id;i<=il;i++)
			{
				cou= snake.courbure(i);
				if(kval) IJ.write("Curvature : "+i+" "+cou);
			}
		}
	}
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}
		return DOES_8G + ROI_REQUIRED;

	}

	void showAbout() {
		IJ.showMessage("About Snake...",
				"This plug-in filter performs a snake segmentation\n"
						+ "from a defined ROI \n"
						+ "and display curvature profile.\n\n"
						+ "Written by tboudier@snv.jussieu.fr");

	}

	class Point2d {
		double x;
		double y;

		public Point2d() {
			x = 0.0;
			y = 0.0;
		}
	}

	class Snake {
		Point2d points[];
		Point2d normale[];
		Point2d deplace[];
		double lambda[];
		int etat[];
		int NPT;
		int NMAX = 5000;
		double maxr, minr;
		int block, elimination,ARRET;
		boolean closed;
		
		// Get number of points
		public int getNbPoints() {
			return NPT;
		}

		// Is the snake closed
		public boolean closed() {
			return closed;
		}
		
		// Draw the snake
		public void DrawSnake(ImageProcessor A) {
			int i;
			int x, y;
			int color;

			for (i = 1; i <= NPT; i++) {
				x = (int) (points[i].x);
				y = (int) (points[i].y);
				if (A.getPixel(x, y) > 127)
					color = 0;
				else
					color = 255;
				A.putPixel(x, y, color);
			}
		}
		
		// Initialization of the snake points
		public void Init(Roi R) {
			Double pos;
			double Rx, Ry;
			int i = 1;
			double a;
			NPT = 0;

			points = new Point2d[NMAX];
			normale = new Point2d[NMAX];
			deplace = new Point2d[NMAX];
			etat = new int[NMAX];
			lambda = new double[NMAX];

			for (i = 1; i < NMAX; i++) {
				points[i] = new Point2d();
				normale[i] = new Point2d();
				deplace[i] = new Point2d();
			}

			if ((R.getType() == Roi.OVAL) || (R.getType() == Roi.RECTANGLE)) {
				closed = true;
				Rectangle Rect = R.getBoundingRect();
				int xc = Rect.x + Rect.width / 2;
				int yc = Rect.y + Rect.height / 2;
				Rx = Rect.width / 2;
				Ry = Rect.height / 2;
				double theta = 2.0 / (double) ((Rx + Ry) / 2);

				i = 1;
				for (a = 0.0; a < 2 * Math.PI; a += theta) {
					points[i].x = (int) (xc + Rx * Math.cos(a));
					points[i].y = (int) (yc + Ry * Math.sin(a));
					NPT++;
					etat[i] = 0;
					i++;
				}

				NPT--;
			}// Rectangle
			else if (R.getType() == Roi.LINE) {
				closed = false;
				Line l = (Line) R;
				Rx = (l.x2 - l.x1);
				Ry = (l.y2 - l.y1);
				a = Math.sqrt(Rx * Rx + Ry * Ry);
				Rx /= a;
				Ry /= a;
				int ind = 1;
				for (i = 0; i <= l.getLength(); i++) {
					points[ind].x = l.x1 + Rx * i;
					points[ind].y = l.y1 + Ry * i;
					etat[ind] = 0;
					ind++;

				}
				NPT = ind - 1;
			}// Line
			else
				IJ.write("Selection type not supported");
			block = 0;
			elimination = 0;
			ARRET = 0;
		}
		
		// regularisation of distance between points
		void bouche_trou() {
			Point2d temp[];
			Point2d Ta;
			int i, j, k, ii, aj;
			double Dmoy, Dtot, DD, Dmin, Dmax, Di, Dmoyg, normtan, D, D1;

			temp = new Point2d[NMAX];
			Ta = new Point2d();

			Dtot = 0.0;
			Dmin = 1000.0;
			Dmax = 0.0;
			for (i = 2; i <= NPT - 1; i++) {
				Di = loinsnake(i, i - 1);
				Dtot += Di;
				if (Di < Dmin)
					Dmin = Di;
				if (Di > Dmax)
					Dmax = Di;
			}
			if ((Dmax / Dmin) > 3.0) {
				Dmoyg = 1.0;
				temp[1] = new Point2d();
				temp[1].x = points[1].x;
				temp[1].y = points[1].y;
				i = 2;
				ii = 2;
				temp[ii] = new Point2d();
				while (i <= NPT) {
					Dmoy = Dmoyg;
					DD = loinsnake(i, i - 1);
					if (DD > Dmoy) {
						aj = (int) (DD / Dmoy);
						Ta.x = points[i].x - points[i - 1].x;
						Ta.y = points[i].y - points[i - 1].y;
						normtan = Math.sqrt(Ta.x * Ta.x + Ta.y * Ta.y);
						Ta.x /= normtan;
						Ta.y /= normtan;
						for (k = 1; k <= aj; k++) {
							temp[ii].x = points[i - 1].x + k * Dmoy * Ta.x;
							temp[ii].y = points[i - 1].y + k * Dmoy * Ta.y;
							ii++;
							temp[ii] = new Point2d();
						}
					}
					i++;
					if ((DD <= Dmoy) && (i < NPT)) {
						j = i - 1;
						D = 0.0;
						while ((D < Dmoy) && (j < NPT)) {
							D += loinsnake(j, j + 1);
							j++;
						}
						temp[ii].x = points[j].x;
						temp[ii].y = points[j].y;
						ii++;
						temp[ii] = new Point2d();
						i = j + 1;
					}
					if (i == NPT) {
						i = NPT + 1;
					}
				}
				temp[ii].x = points[NPT].x;
				temp[ii].y = points[NPT].y;
				NPT = ii;
				for (i = 1; i <= NPT; i++) {
					points[i].x = temp[i].x;
					points[i].y = temp[i].y;
				}
			}

		}

		// main calculus function
		void calculus(int deb, int fin) {
			int i;
			Point2d bi, temp, debtemp;
			double mi, gi, di;
			double omega;

			omega = 1.8;
			bi = new Point2d();
			temp = new Point2d();
			debtemp = new Point2d();

			debtemp.x = points[deb].x;
			debtemp.y = points[deb].y;

			for (i = deb; i < fin; i++) {
				bi.x = points[i].x + deplace[i].x;
				bi.y = points[i].y + deplace[i].y;
				gi = -lambda[i] * lambda[i + 1] - (lambda[i] * lambda[i]);
				di = -lambda[i] * lambda[i + 1]
						- (lambda[i + 1] * lambda[i + 1]);
				mi = (lambda[i] * lambda[i]) + 2.0 * lambda[i] * lambda[i + 1]
						+ (lambda[i + 1] * lambda[i + 1]) + 1.0;
				if (i > deb) {
					temp.x = mi
							* points[i].x
							+ omega
							* (-gi * points[i - 1].x - mi * points[i].x - di
									* points[i + 1].x + bi.x);
					temp.y = mi
							* points[i].y
							+ omega
							* (-gi * points[i - 1].y - mi * points[i].y - di
									* points[i + 1].y + bi.y);
				}
				if ((i == deb) && (closed)) {
					temp.x = mi
							* points[i].x
							+ omega
							* (-gi * points[fin].x - mi * points[i].x - di
									* points[i + 1].x + bi.x);
					temp.y = mi
							* points[i].y
							+ omega
							* (-gi * points[fin].y - mi * points[i].y - di
									* points[i + 1].y + bi.y);
				}
				if ((i == deb) && (!closed)) {
					temp.x = points[deb].x * mi;
					temp.y = points[deb].y * mi;
				}
				points[i].x = temp.x / mi;
				points[i].y = temp.y / mi;
			}
			// LAST POINT
			if (closed) {
				i = fin;
				bi.x = points[i].x + deplace[i].x;
				bi.y = points[i].y + deplace[i].y;
				gi = -lambda[i] * lambda[deb] - (lambda[i] * lambda[i]);
				di = -lambda[i] * lambda[deb] - (lambda[deb] * lambda[deb]);
				mi = (lambda[i] * lambda[i]) + 2.0 * lambda[i] * lambda[deb]
						+ (lambda[deb] * lambda[deb]) + 1.0;
				temp.x = mi
						* points[i].x
						+ omega
						* (-gi * points[i - 1].x - mi * points[i].x - di
								* debtemp.x + bi.x);
				temp.y = mi
						* points[i].y
						+ omega
						* (-gi * points[i - 1].y - mi * points[i].y - di
								* debtemp.y + bi.y);
				points[i].x = temp.x / mi;
				points[i].y = temp.y / mi;
			}
		}

		// serach for the closest edge along the normale direction
		Point2d compute_grad(int num, ImageProcessor A, int SEUILBAS,
				int SEUILHAUT, int directions) {
			int moyplus, moymoins;
			int dist = 100;
			int iy, ix;
			int deplus, demoins;
			double posplus, posmoins;
			int scaleint = 50;
			double crp = 2000.0;
			double crm = -2000.0;
			double bres, ares, bden, bnum;
			int i, j;
			Point2d displacement, pos, norm;
			int grad;
			int image_line[] = new int[200];

			pos = points[num];
			norm = normale[num];

			displacement = new Point2d();

			for (i = 0; i < 2 * scaleint; i++) {
				iy = (int) (pos.y + norm.y * (i - scaleint));
				ix = (int) (pos.x + norm.x * (i - scaleint));
				if (ix < 0) {
					ix = 0;
				}
				if (iy < 0) {
					iy = 0;
				}
				if (ix > A.getWidth() - 1) {
					ix = A.getWidth() - 1;
				}
				if (iy > A.getHeight() - 1) {
					iy = A.getHeight() - 1;
				}
				image_line[i] = A.getPixel(ix, iy);
			}

			for (i = 1; i < NPT; i++) {
				if ((i != num) && (i != num - 1)) {
					bden = (-norm.x * points[i + 1].y + norm.x * points[i].y
							+ norm.y * points[i + 1].x - norm.y * points[i].x);
					bnum = (-norm.x * pos.y + norm.x * points[i].y + norm.y
							* pos.x - norm.y * points[i].x);

					if (bden != 0)
						bres = (bnum / bden);
					else
						bres = 5.0;
					if ((bres >= 0.0) && (bres <= 1.0)) {
						ares = -(-points[i + 1].y * pos.x + points[i + 1].y
								* points[i].x + points[i].y * pos.x + pos.y
								* points[i + 1].x - pos.y * points[i].x - points[i].y
								* points[i + 1].x)
								/ (-norm.x * points[i + 1].y + norm.x
										* points[i].y + norm.y
										* points[i + 1].x - norm.y
										* points[i].x);
						if ((ares > 0.0) && (ares < crp)) {
							crp = ares;
						}
						if ((ares < 0.0) && (ares > crm)) {
							crm = ares;
						}
					}
				}
			}
			crp = crp * 0.75;
			crm = crm * 0.75;
			crp = crp * 1.0;
			crm = crm * 1.0;
			deplus = 1000;
			demoins = -1000;
			for (i = 2; i < 2 * scaleint - 3; i++) {
				grad = Math.abs(image_line[i - 1] - image_line[i]);
				moyplus = (int) (0.33 * (image_line[i] + image_line[i + 1] + image_line[i + 2]));
				moymoins = (int) (0.33 * (image_line[i] + image_line[i - 1] + image_line[i - 2]));
				if ((moymoins >= SEUILHAUT) && (moyplus <= SEUILBAS)) {
					dist = (i - scaleint);
					if ((dist < 0) && (dist > demoins)) {
						demoins = dist;
					}
					if ((dist >= 0) && (dist < deplus)) {
						deplus = dist;
					}
				}
			}
			etat[num] = 0;
			posplus = (double) (deplus);
			posmoins = (double) (demoins);
			if (((deplus == 1000) && (demoins == -1000))
					|| ((posplus > crp) && (posmoins < crm))
					|| ((deplus == 1000) && (posmoins < crm))
					|| ((demoins == -1000) && (posplus > crp))) {
				etat[num] = 1;
				elimination = 1;
				displacement.x = 0.0;
				displacement.y = 0.0;
			}
			if (((deplus < 1000) && (posplus < crp))
					&& ((demoins == -1000) || (posmoins < crm) || (posplus < -posmoins))) {
				displacement.x = norm.x * (double) (deplus);
				displacement.y = norm.y * (double) (deplus);
				if (deplus < 2) {
				}
			}
			if (((demoins > -1000) && (posmoins > crm))
					&& ((deplus == 1000) || (posplus > crp) || (posmoins > -posplus))) {
				displacement.x = norm.x * (double) (demoins);
				displacement.y = norm.y * (double) (demoins);
				if (demoins > -2) {
				}
			}

			if (directions == 1) {
				if (deplus != 1000) {
					displacement.x = norm.x * (double) (deplus);
					displacement.y = norm.y * (double) (deplus);
				} else {
					displacement.x = 0.0;
					displacement.y = 0.0;
				}
			}
			return (displacement);
		}

		// compute normale
		Point2d compute_normale(int np) {

			Point2d norma, tan;
			double normtan;

			tan = new Point2d();
			norma = new Point2d();

			if (np == 1) {
				if (closed) {
					tan.x = points[2].x - points[NPT].x;
					tan.y = points[2].y - points[NPT].y;
				} else {
					tan.x = points[2].x - points[1].x;
					tan.y = points[2].y - points[1].y;
				}
			}
			if (np == NPT) {
				if (closed) {
					tan.x = points[1].x - points[NPT - 1].x;
					tan.y = points[1].y - points[NPT - 1].y;
				} else {
					tan.x = points[NPT].x - points[NPT - 1].x;
					tan.y = points[NPT].y - points[NPT - 1].y;
				}
			}
			if ((np > 1) && (np < NPT)) {
				tan.x = points[np + 1].x - points[np - 1].x;
				tan.y = points[np + 1].y - points[np - 1].y;
			}
			normtan = Math.sqrt(tan.x * tan.x + tan.y * tan.y);
			if (normtan > 0.0) {
				tan.x /= normtan;
				tan.y /= normtan;
				norma.x = -tan.y;
				norma.y = tan.x;
			}
			return (norma);

		}

		// curvature computation
		public double courbure(int iref) {

			double dl, da, ares, a;
			Point2d U, V, W, pos, norm;
			int scale = 9;
			int i = iref;

			U = new Point2d();
			V = new Point2d();
			W = new Point2d();
			pos = new Point2d();
			norm = new Point2d();

			if ((iref > scale) && (iref <= NPT - scale)) {
				U.x = points[i - scale].x - points[i].x;
				U.y = points[i - scale].y - points[i].y;
				V.x = points[i].x - points[i + scale].x;
				V.y = points[i].y - points[i + scale].y;
				W.x = points[i - scale].x - points[i + scale].x;
				W.y = points[i - scale].y - points[i + scale].y;
				pos.x = (points[i - scale].x + points[i].x + points[i + scale].x) / 3;
				pos.y = (points[i - scale].y + points[i].y + points[i + scale].y) / 3;
			}
			if ((iref <= scale) && (closed)) {
				U.x = points[NPT + i - scale].x - points[i].x;
				U.y = points[NPT + i - scale].y - points[i].y;
				V.x = points[i].x - points[i + scale].x;
				V.y = points[i].y - points[i + scale].y;
				W.x = points[NPT + i - scale].x - points[i + scale].x;
				W.y = points[NPT + i - scale].y - points[i + scale].y;
				pos.x = (points[NPT + i - scale].x + points[i].x + points[i
						+ scale].x) / 3;
				pos.y = (points[NPT + i - scale].y + points[i].y + points[i
						+ scale].y) / 3;
			}
			if ((iref > NPT - scale) && (closed)) {
				U.x = points[i - scale].x - points[i].x;
				U.y = points[i - scale].y - points[i].y;
				V.x = points[i].x - points[(i + scale) % NPT].x;
				V.y = points[i].y - points[(i + scale) % NPT].y;
				W.x = points[i - scale].x - points[(i + scale) % NPT].x;
				W.y = points[i - scale].y - points[(i + scale) % NPT].y;
				pos.x = (points[i - scale].x + points[i].x + points[(i + scale)
						% NPT].x) / 3;
				pos.y = (points[i - scale].y + points[i].y + points[(i + scale)
						% NPT].y) / 3;
			}
			double l = Math.sqrt(W.x * W.x + W.y * W.y);
			da = ((U.x * V.x + U.y * V.y) / ((Math.sqrt(U.x * U.x + U.y * U.y) * (Math
					.sqrt(V.x * V.x + V.y * V.y)))));
			a = Math.acos(da);
			if (!inside(pos)) {
				return (-1.0 * a / l);
			} else {
				return (a / l);
			}
		}

		// Fourier descriptor X coeff a
		public double FourierDXa(int k) {

			double som = 0.0;

			for (int i = 1; i <= NPT; i++)
				som += points[i].x * Math.cos(2 * k * Math.PI * i / NPT);
			return (som * 2 / NPT);
		}

		// Fourier descriptors X coeff b
		public double FourierDXb(int k) {

			double som = 0.0;

			for (int i = 1; i <= NPT; i++)
				som += points[i].x * Math.sin(2 * k * Math.PI * i / NPT);
			return (som * 2 / NPT);
		}

		// Fourier descriptors Y coeff a
		public double FourierDYa(int k) {

			double som = 0.0;

			for (int i = 1; i <= NPT; i++)
				som += points[i].y * Math.cos(2 * k * Math.PI * i / NPT);
			return (som * 2 / NPT);
		}

		// Fourier descriptors Y coeff b
		public double FourierDYb(int k) {

			double som = 0.0;

			for (int i = 1; i <= NPT; i++)
				som += points[i].y * Math.sin(2 * k * Math.PI * i / NPT);
			return (som * 2 / NPT);
		}

		// draw fourier dexcriptor
		public void drawFourier(ImageProcessor A, int kmax) {
			double posx, posy;
			double ax[] = new double[kmax + 1];
			double bx[] = new double[kmax + 1];
			double ay[] = new double[kmax + 1];
			double by[] = new double[kmax + 1];
			double tempx[] = new double[NPT + 1];
			double tempy[] = new double[NPT + 1];

			for (int i = 0; i <= kmax; i++) {
				ax[i] = FourierDXa(i);
				bx[i] = FourierDXb(i);
				ay[i] = FourierDYa(i);
				by[i] = FourierDYb(i);
			}
			for (int l = 1; l <= NPT; l++) {
				posx = ax[0] / 2.0;
				posy = ay[0] / 2.0;
				for (int k = 1; k <= kmax; k++) {
					posx += ax[k] * Math.cos(2 * Math.PI * k * l / NPT) + bx[k]
							* Math.sin(2 * Math.PI * k * l / NPT);
					posy += ay[k] * Math.cos(2 * Math.PI * k * l / NPT) + by[k]
							* Math.sin(2 * Math.PI * k * l / NPT);
				}
				A.putPixel((int) posx, (int) posy, 0);
				tempx[l] = posx;
				tempy[l] = posy;
			}
			for (int i = 1; i <= NPT; i++) {
				points[i].x = tempx[i];
				points[i].y = tempy[i];
			}

		}

		// destruction
		void destroysnake() {
			Point2d temp[], fo[];
			double lan[];
			int state[];
			int i, j;

			temp = new Point2d[NPT];
			fo = new Point2d[NPT];
			lan = new double[NPT];
			state = new int[NPT];

			j = 1;
			for (i = 1; i <= NPT; i++) {
				if (etat[i] != 1) {
					temp[j] = new Point2d();
					temp[j].x = points[i].x;
					temp[j].y = points[i].y;
					state[j] = etat[i];
					fo[j] = new Point2d();
					fo[j].x = deplace[i].x;
					fo[j].y = deplace[i].y;
					lan[j] = lambda[i];
					j++;
				}
			}
			NPT = j - 1;

			for (i = 1; i <= NPT; i++) {
				points[i].x = temp[i].x;
				points[i].y = temp[i].y;
				etat[i] = state[i];
				deplace[i].x = fo[i].x;
				deplace[i].y = fo[i].y;
				lambda[i] = lan[i];
			}
		}

		// distance two points of the snake
		double loinsnake(int a, int b) {
			return (Math.sqrt(Math.pow(points[a].x - points[b].x, 2.0)
					+ Math.pow(points[a].y - points[b].y, 2.0)));

		}

		// compute new positions of the snake
		void nouv() {
			calculus(1, NPT);

		}

		// main function for the snake
		public void principal(ImageProcessor A, double Divforce, int seuilb,
				int seuilh, double minr, double maxr) {
			int block0;
			int i, j;
			double force, DF;
			Point2d displ = new Point2d();
			double maxforce = 0.0;

			DF = Divforce;
			for (i = 1; i <= NPT; i++) {
				normale[i] = compute_normale(i);
			}
			block = 0;
			elimination = 0;
			for (i = 1; i <= NPT; i++) {
				displ.x = 0.0;
				displ.y = 0.0;
				displ = compute_grad(i, A, seuilb, seuilh, 2);
				deplace[i].x = (displ.x) / Divforce;
				deplace[i].y = (displ.y) / Divforce;
				force = 0.0;
				force = Math.sqrt(Math.pow(deplace[i].x, 2.0)
						+ Math.pow(deplace[i].y, 2.0));
				if (force > maxforce) {
					maxforce = force;
				}
			}
			maxforce = Math.sqrt(maxforce);
			block0 = block;
			if (block >= NPT - 1) {
				ARRET = 5;
			}
			if (((block0 - block) < 5) && (block > (int) (NPT * 5.5 / 10.0))) {
				ARRET++;
			}

			for (i = 1; i <= NPT; i++) {
				force = Math.sqrt(deplace[i].x * deplace[i].x + deplace[i].y
						* deplace[i].y);
				if (force > 0.0)
					lambda[i] = maxr
							/ (1.0 + ((maxr - minr) / minr)
									* (force / maxforce));
				else
					lambda[i] = maxr;

			}
			if (elimination == 1) {
				destroysnake();
			}
			nouv();
			bouche_trou();
		}

		// /////////////////////////////////////////////
		// //SEGMENTATION : inside/outside the snake///
		// /////////////////////////////////////////////
		public void segmentation(ImageProcessor A) {
			Point2d pos, norm, ref;
			int top, left, right, bottom;
			int i, j;
			int x, y, count;
			double bden, bnum, bres, lnorm, ares;

			pos = new Point2d();
			norm = new Point2d();
			ref = new Point2d();
			top = 0;
			bottom = 1000;
			left = 1000;
			right = 0;
			for (i = 1; i <= NPT; i++) {
				if (points[i].y > top)
					top = (int) points[i].y;
				if (points[i].y < bottom)
					bottom = (int) points[i].y;
				if (points[i].x > right)
					right = (int) points[i].x;
				if (points[i].x < left)
					left = (int) points[i].x;
			}
			ref.x = 0;
			ref.y = 0;
			for (x = left; x < right; x++)
				for (y = bottom; y < top; y++) {
					pos.x = x;
					pos.y = y;
					norm.x = ref.x - pos.x;
					norm.y = ref.y - pos.y;
					lnorm = Math.sqrt(norm.x * norm.x + norm.y * norm.y);
					norm.x /= lnorm;
					norm.y /= lnorm;

					if (inside(pos))
						if ((x % 3 != 0) && (y % 3 != 0))
							A.putPixel(x, y, 255 - A.getPixel(x, y));
				}
		}

		boolean inside(Point2d pos) {
			int count, i;
			double bden, bnum, bres, ares, lnorm;
			Point2d norm = new Point2d();
			Point2d ref = new Point2d();

			ref.x = 0.0;
			ref.y = 0.0;
			norm.x = ref.x - pos.x;
			norm.y = ref.y - pos.y;
			lnorm = Math.sqrt(norm.x * norm.x + norm.y * norm.y);
			norm.x /= lnorm;
			norm.y /= lnorm;

			count = 0;
			for (i = 1; i < NPT; i++) {
				bden = (-norm.x * points[i + 1].y + norm.x * points[i].y
						+ norm.y * points[i + 1].x - norm.y * points[i].x);
				bnum = (-norm.x * pos.y + norm.x * points[i].y + norm.y * pos.x - norm.y
						* points[i].x);
				if (bden != 0)
					bres = (bnum / bden);
				else
					bres = 5.0;
				if ((bres >= 0.0) && (bres <= 1.0)) {
					ares = -(-points[i + 1].y * pos.x + points[i + 1].y
							* points[i].x + points[i].y * pos.x + pos.y
							* points[i + 1].x - pos.y * points[i].x - points[i].y
							* points[i + 1].x)
							/ (-norm.x * points[i + 1].y + norm.x * points[i].y
									+ norm.y * points[i + 1].x - norm.y
									* points[i].x);
					if ((ares > 0.0) && (ares < lnorm))
						count++;
				}
			}
			return (count % 2 == 1);

		}
	}
}
