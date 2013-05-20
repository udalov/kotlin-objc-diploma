@interface NSObject
@end

@interface A : NSObject
- (void *) pv1;
- (const void *) pv2;
- (void * const) pv3;
- (const void * const) pv4;

- (void **) ppv;
- (void ***) pppv;

- (int *) pi;
- (char *) pc;
- (long *) pj;
- (short *) ps;
- (double *) pd;
- (float *) pf;

- (A **) pa;

- (void (**)()) pfun;
@end
