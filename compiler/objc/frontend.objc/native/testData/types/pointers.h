@interface A
- (void *) pv;
- (int *) pi;
- (id *) poi;
- (Class *) poc;
- (SEL *) pos;

- (void * const) pv2;
- (const void *) pv3;
- (const void * const) pv4;

- (void **) ppv;

- (int *const *) ppi;
- (const int *const *) ppi2;
- (const int *const *const) ppi3;
@end
