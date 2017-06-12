function putPixel(ctx, x, y) 
{
    ctx.beginPath();
    ctx.strokeRect(x, y, 1, 1);
}

// Mid-point Line drawing algorithm
function drawLine(ctx, x1, y1, x2, y2)
{
	var decision;
    var increment_y, increment_e, increment_ne;
    var is_slope_gt1 = false;
	var dx = Math.abs(x1-x2);
	var dy = Math.abs(y1-y2);

	if(dy > dx) {
        y1 = [x1, x1 = y1][0];  // swap x1, y1
        y2 = [x2, x2 = y2][0];  // swap x2, y2
        dy = [dx, dx = dy][0];  // swap dx, dy
		is_slope_gt1 = true;
	}

	if(x1>x2) {
        x2 = [x1, x1 = x2][0];  // swap x1, x2
        y2 = [y1, y1 = y2][0];  // swap y1, y2
	}

    decision     = 2 * dy - dx;
	increment_e  = 2 * dy;
	increment_ne = 2 * (dy - dx);
    increment_y  = y1 > y2 ? -1 : 1;

	while(x1 < x2) {
		if(decision <= 0)
			decision += increment_e;
		else {
			decision += increment_ne;
			y1       += increment_y;
		}

		x1++;
		if(is_slope_gt1)
			putPixel(ctx, y1, x1);
		else
			putPixel(ctx, x1, y1);
	}
}

// Mid-point Circle drawing algorithm
function drawCircle(ctx, xc, yc, radius)
{
    var x = radius;
    var y = 0;
    var error = 0;
 
    while (x >= y)
    {
        putPixel(ctx, xc + x, yc + y);
        putPixel(ctx, xc + y, yc + x);
        putPixel(ctx, xc - y, yc + x);
        putPixel(ctx, xc - x, yc + y);
        putPixel(ctx, xc - x, yc - y);
        putPixel(ctx, xc - y, yc - x);
        putPixel(ctx, xc + y, yc - x);
        putPixel(ctx, xc + x, yc - y);
 
        if (error <= 0) {
            y += 1;
            error += 2 * y + 1;
        } else {
            x -= 1;
            error -= 2 * x + 1;
        }
    }
}

// Mid-point Ellipse drawing algorithm
function drawEllipse(ctx, xc, yc, rx, ry) 
{ 
    var x = 0
    var y = ry
    var p = (ry * ry) - (rx * rx * ry) + ((rx * rx)/4); 

    while((2 * x * ry * ry)< (2 * y * rx * rx)) { 
	    putPixel(ctx, xc + x, yc - y); 
	    putPixel(ctx, xc - x, yc + y); 
	    putPixel(ctx, xc + x, yc + y); 
	    putPixel(ctx, xc - x, yc - y); 

        if(p < 0) { 
            x  = x+1; 
            p += (2 * ry * ry *x) + (ry * ry); 
        } else { 
            x++; 
            y-=1; 
            p += (2 * ry * ry * x + ry * ry)-(2 * rx * rx *y); 
        } 
    }    

    p = (x + 0.5) * (x + 0.5) * ry * ry + (y - 1) * (y - 1) * rx * rx - rx * rx * ry * ry; 
    while(y >= 0) { 
	    putPixel(ctx, xc + x, yc - y); 
	    putPixel(ctx, xc - x, yc + y); 
	    putPixel(ctx, xc + x, yc + y); 
	    putPixel(ctx, xc - x, yc - y); 
	
	    if(p > 0) { 
		    y--; 
		    p -= (2 * rx * rx *y) + (rx * rx); 
	    } else { 
		    y--; 
		    x++; 
            p += (2 * ry * ry * x) - (2 * rx * rx * y)-(rx * rx); 
	    } 
	} 
}

function drawRectangle(ctx, x1, y1, x2, y2)
{
    drawLine(ctx, x1, y1, x2, y1);  // top
    drawLine(ctx, x1, y2, x2, y2);  // bottom
    drawLine(ctx, x1, y1, x1, y2);  // left
    drawLine(ctx, x2, y1, x2, y2);  // right
}

function drawPolygon(ctx, points, isClosed)
{
    var point = points[0]; 

    for (var i = 1, len = points.length; i < len; i++) {
        var nextPoint = points[i];
        drawLine(ctx, point.x, point.y, nextPoint.x, nextPoint.y);
        point = nextPoint;
    }

    if (isClosed)
        // For Polygon (closed) just draw one extra edge
        // that connects first and last vertex
        drawLine(ctx, points[0].x, points[0].y, points[points.length-1].x, points[points.length-1].y);
}