// Converts a signed 16-bit image to unsigned.

  imp = IJ.getImage();
  cal = imp.getCalibration();
  if (!cal.isSigned16Bit())
     IJ.error("Signed 16-bit image required");
  cal.disableDensityCalibration();
  ip = imp.getProcessor();
  min = ip.getMin();
  max = ip.getMax();
  stats = new StackStatistics(imp);
  minv = stats.min;
  stack = imp.getStack();
  for (i=1; i<=stack.getSize(); i++) {
     ip = stack.getProcessor(i);
     ip.add(-minv);
  }
  imp.setStack(stack);
  ip = imp.getProcessor();
  ip.setMinAndMax(min-minv, max-minv);
  imp.updateAndDraw();
