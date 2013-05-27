#import <Foundation/NSObject.h>

typedef int (*fun_t)();
typedef fun_t (*megafun_t)();

@interface A : NSObject
+ (void) printOKIf42: (megafun_t) megafun;
@end
