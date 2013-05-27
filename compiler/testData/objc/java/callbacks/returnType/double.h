#import <Foundation/NSObject.h>

typedef double (*fun_t)();

@interface A : NSObject
+ (void) printOKIfPi: (fun_t) fun;
@end
