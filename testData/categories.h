@interface A @end
@interface B @end
@interface C @end


@interface A()
@end


@interface B(B)
-(void) foo;
-(int) bar;
@end


@interface C(CC)
+(void) foo;
+(int) bar;
@end
